package com.serviceconnect.service;

import com.serviceconnect.config.PesapalProperties;
import com.serviceconnect.dto.PesapalApiDtos;
import com.serviceconnect.dto.request.InitiatePaymentRequest;
import com.serviceconnect.dto.response.InitiatePaymentResponse;
import com.serviceconnect.entity.Technician;
import com.serviceconnect.entity.TechnicianPayment;
import com.serviceconnect.event.TechnicianAvailableEvent;
import com.serviceconnect.exception.BadRequestException;
import com.serviceconnect.exception.ResourceNotFoundException;
import com.serviceconnect.repository.TechnicianPaymentRepository;
import com.serviceconnect.repository.TechnicianRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentGatewayService {

    private final PesapalService pesapalService;
    private final TechnicianRepository technicianRepository;
    private final TechnicianPaymentRepository paymentRepository;
    private final PesapalProperties pesapalProperties;
    private final ActivityLogService activityLogService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public InitiatePaymentResponse initiatePayment(InitiatePaymentRequest request, String userEmail, String userPhone, String userName) {
        Technician technician = technicianRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found: " + request.getUserId()));

        Double amount = request.getAmount();
        if (amount == null) {
            amount = request.getType() == InitiatePaymentRequest.PaymentType.REGISTRATION
                    ? pesapalProperties.getRegistrationFeeAmount()
                    : pesapalProperties.getMonthlySubscriptionAmount();
        }

        String orderId = UUID.randomUUID().toString();

        TechnicianPayment payment = TechnicianPayment.builder()
                .technicianId(technician.getId())
                .amount(amount)
                .paymentType(mapPaymentType(request.getType()))
                .status(TechnicianPayment.PaymentStatus.PENDING)
                .reference(orderId)
                .dueDate(LocalDateTime.now().plusDays(7))
                .build();
        paymentRepository.save(payment);

        PesapalApiDtos.SubmitOrderRequest pesapalRequest = new PesapalApiDtos.SubmitOrderRequest();
        pesapalRequest.setId(orderId);
        pesapalRequest.setAmount(amount);
        pesapalRequest.setDescription(request.getType().name().equals("REGISTRATION") ? "Technician Registration Fee" : "Monthly Subscription");
        pesapalRequest.setCallbackUrl(pesapalProperties.getCallbackUrl());
        pesapalRequest.setNotificationId(orderId); // Use orderId as notificationId

        PesapalApiDtos.SubmitOrderRequest.BillingAddress billingAddress = new PesapalApiDtos.SubmitOrderRequest.BillingAddress();
        billingAddress.setEmailAddress(userEmail != null ? userEmail : technician.getEmail());
        billingAddress.setPhoneNumber(userPhone != null ? userPhone : technician.getPhone());
        String fullName = userName != null ? userName : technician.getName();
        String[] nameParts = fullName.split(" ", 2);
        billingAddress.setFirstName(nameParts[0]);
        billingAddress.setLastName(nameParts.length > 1 ? nameParts[1] : "");
        pesapalRequest.setBillingAddress(billingAddress);

        PesapalApiDtos.SubmitOrderResponse response = pesapalService.submitOrder(pesapalRequest);

        payment.setOrderTrackingId(response.getOrderTrackingId());
        paymentRepository.save(payment);

        return InitiatePaymentResponse.builder()
                .paymentLink(response.getRedirectUrl())
                .reference(orderId)
                .amount(amount)
                .type(request.getType().name())
                .currency("TZS")
                .build();
    }

    @Transactional
    public void processSuccessfulPayment(String orderTrackingId) {
        Optional<TechnicianPayment> optionalPayment = paymentRepository.findByOrderTrackingId(orderTrackingId);
        if (optionalPayment.isEmpty()) {
            log.warn("Payment not found for orderTrackingId: {}", orderTrackingId);
            return;
        }

        TechnicianPayment payment = optionalPayment.get();
        if (payment.getStatus() == TechnicianPayment.PaymentStatus.SUCCESS) {
            log.info("Payment already processed for orderTrackingId: {}", orderTrackingId);
            return;
        }

        payment.setStatus(TechnicianPayment.PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        Technician technician = technicianRepository.findById(payment.getTechnicianId())
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found: " + payment.getTechnicianId()));

        boolean becameAvailable = false;
        LocalDate now = LocalDate.now();
        if (payment.getPaymentType() == TechnicianPayment.PaymentType.REGISTRATION) {
            technician.setAccountStatus(Technician.AccountStatus.ACTIVE);
            technician.setSubscriptionExpiryAt(now.plusMonths(1));
            technician.setAvailability(Technician.Availability.available);
            technician.setStatus(Technician.ApprovalStatus.approved);
            becameAvailable = true;
            activityLogService.log(technician.getId(), technician.getName(), "technician",
                    "REGISTRATION_PAYMENT_SUCCESS", "Registration payment completed via Pesapal");
            createNextSubscriptionPayment(technician.getId());
        } else if (payment.getPaymentType() == TechnicianPayment.PaymentType.MONTHLY_SUBSCRIPTION) {
            LocalDate currentExpiry = technician.getSubscriptionExpiryAt();
            if (currentExpiry != null && currentExpiry.isAfter(now)) {
                technician.setSubscriptionExpiryAt(currentExpiry.plusMonths(1));
            } else {
                technician.setSubscriptionExpiryAt(now.plusMonths(1));
            }
            technician.setAccountStatus(Technician.AccountStatus.ACTIVE);
            if (technician.getAvailability() != Technician.Availability.available) {
                technician.setAvailability(Technician.Availability.available);
                becameAvailable = true;
            }
            activityLogService.log(technician.getId(), technician.getName(), "technician",
                    "MONTHLY_SUBSCRIPTION_PAYMENT_SUCCESS", "Monthly subscription renewed via Pesapal");
            createNextSubscriptionPayment(technician.getId());
        }

        technicianRepository.save(technician);

        if (becameAvailable) {
            eventPublisher.publishEvent(new TechnicianAvailableEvent(this, technician.getId()));
        }
    }

    private void createNextSubscriptionPayment(Long technicianId) {
        TechnicianPayment nextPayment = TechnicianPayment.builder()
                .technicianId(technicianId)
                .amount(pesapalProperties.getMonthlySubscriptionAmount())
                .paymentType(TechnicianPayment.PaymentType.MONTHLY_SUBSCRIPTION)
                .status(TechnicianPayment.PaymentStatus.PENDING)
                .dueDate(LocalDateTime.now().plusMonths(1))
                .build();
        paymentRepository.save(nextPayment);
    }

    private TechnicianPayment.PaymentType mapPaymentType(InitiatePaymentRequest.PaymentType type) {
        return type == InitiatePaymentRequest.PaymentType.REGISTRATION
                ? TechnicianPayment.PaymentType.REGISTRATION
                : TechnicianPayment.PaymentType.MONTHLY_SUBSCRIPTION;
    }

    @Transactional
    public int suspendExpiredAccounts() {
        List<Technician> activeTechnicians = technicianRepository.findByAccountStatus(Technician.AccountStatus.ACTIVE);
        int suspendedCount = 0;
        LocalDate now = LocalDate.now();
        for (Technician technician : activeTechnicians) {
            if (technician.getSubscriptionExpiryAt() != null && technician.getSubscriptionExpiryAt().isBefore(now)) {
                technician.setAccountStatus(Technician.AccountStatus.SUSPENDED);
                technician.setAvailability(Technician.Availability.offline);
                technicianRepository.save(technician);
                activityLogService.log(technician.getId(), technician.getName(), "technician",
                        "ACCOUNT_SUSPENDED_EXPIRED", "Account suspended due to expired subscription");
                suspendedCount++;
            }
        }
        return suspendedCount;
    }
}
