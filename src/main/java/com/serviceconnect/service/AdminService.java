package com.serviceconnect.service;

import com.serviceconnect.dto.response.ActivityLogResponse;
import com.serviceconnect.dto.response.UserResponse;
import com.serviceconnect.entity.ServiceRequest;
import com.serviceconnect.entity.Technician;
import com.serviceconnect.entity.TechnicianPayment;
import com.serviceconnect.event.TechnicianAvailableEvent;
import com.serviceconnect.exception.BadRequestException;
import com.serviceconnect.exception.ResourceNotFoundException;
import com.serviceconnect.repository.ServiceRequestRepository;
import com.serviceconnect.repository.TechnicianRepository;
import com.serviceconnect.repository.TechnicianPaymentRepository;
import com.serviceconnect.util.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final TechnicianRepository technicianRepository;
    private final TechnicianPaymentRepository paymentRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final ActivityLogService activityLogService;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;

    public Page<UserResponse> getTechnicians(String status, Pageable pageable) {
        Page<Technician> technicians;
        if (status != null && !status.isBlank()) {
            try {
                Technician.ApprovalStatus s = Technician.ApprovalStatus.valueOf(status);
                technicians = technicianRepository.findByStatus(s, pageable);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status value: " + status);
            }
        } else {
            technicians = technicianRepository.findAll(pageable);
        }
        return technicians.map(userMapper::toResponse);
    }

    // Keep the old method for backward compatibility
    public List<UserResponse> getTechnicians(String status) {
        return getTechnicians(status, Pageable.unpaged()).getContent();
    }

    public UserResponse getTechnicianById(Long id) {
        Technician tech = technicianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found with id: " + id));
        return userMapper.toResponse(tech);
    }

    @Transactional
    public UserResponse approveTechnician(Long id, Long adminId, String adminName) {
        Technician tech = technicianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found with id: " + id));

        if (tech.getStatus() == Technician.ApprovalStatus.approved) {
            throw new BadRequestException("Technician is already approved");
        }

        tech.setStatus(Technician.ApprovalStatus.approved);
        if (tech.getAccountStatus() == Technician.AccountStatus.ACTIVE) {
            tech.setAvailability(Technician.Availability.available);
        } else {
            tech.setAvailability(Technician.Availability.offline);
        }
        Technician saved = technicianRepository.save(tech);

        // Trigger real-time matching if technician is now available
        if (saved.getAvailability() == Technician.Availability.available) {
            try {
                activityLogService.log(adminId, adminName, "admin",
                        "APPROVE_TECHNICIAN_TRIGGER_MATCHING",
                        "Approved technician and triggered real-time matching: " + tech.getName() + " (id=" + id + ")");
                eventPublisher.publishEvent(new TechnicianAvailableEvent(this, saved.getId()));
            } catch (Exception e) {
                log.error("Error triggering matching for approved technician {}: {}", id, e.getMessage());
            }
        }

        activityLogService.log(adminId, adminName, "admin",
                "APPROVE_TECHNICIAN",
                "Approved technician: " + tech.getName() + " (id=" + id + ")");

        return userMapper.toResponse(saved);
    }

    @Transactional
    public UserResponse rejectTechnician(Long id, String reason, Long adminId, String adminName) {
        Technician tech = technicianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found with id: " + id));

        tech.setStatus(Technician.ApprovalStatus.rejected);
        tech.setRejectionReason(reason);
        Technician saved = technicianRepository.save(tech);

        activityLogService.log(adminId, adminName, "admin",
                "REJECT_TECHNICIAN",
                "Rejected technician: " + tech.getName() + " (id=" + id + ") Reason: " + reason);

        return userMapper.toResponse(saved);
    }

    @Transactional
    public UserResponse blockTechnician(Long id, Long adminId, String adminName) {
        Technician tech = technicianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found with id: " + id));

        tech.setAccountStatus(Technician.AccountStatus.BLOCKED);
        tech.setAvailability(Technician.Availability.offline);
        Technician saved = technicianRepository.save(tech);

        activityLogService.log(adminId, adminName, "admin",
                "BLOCK_TECHNICIAN",
                "Blocked technician: " + tech.getName() + " (id=" + id + ")");
        return userMapper.toResponse(saved);
    }

    @Transactional
    public UserResponse unblockTechnician(Long id, Long adminId, String adminName) {
        Technician tech = technicianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found with id: " + id));

        if (tech.getAccountStatus() == Technician.AccountStatus.BLOCKED) {
            tech.setAccountStatus(Technician.AccountStatus.INACTIVE);
        }
        tech.setAvailability(Technician.Availability.offline);
        Technician saved = technicianRepository.save(tech);

        activityLogService.log(adminId, adminName, "admin",
                "UNBLOCK_TECHNICIAN",
                "Unblocked technician: " + tech.getName() + " (id=" + id + ")");
        return userMapper.toResponse(saved);
    }

    @Transactional
    public UserResponse forceActivateTechnician(Long id, Long adminId, String adminName) {
        Technician tech = technicianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found with id: " + id));

        tech.setAccountStatus(Technician.AccountStatus.ACTIVE);
        if (tech.getSubscriptionExpiryAt() == null || tech.getSubscriptionExpiryAt().isBefore(LocalDate.now())) {
            tech.setSubscriptionExpiryAt(LocalDate.now().plusMonths(1));
        }
        if (tech.getStatus() == Technician.ApprovalStatus.approved) {
            tech.setAvailability(Technician.Availability.available);
        }
        Technician saved = technicianRepository.save(tech);

        if (saved.getAvailability() == Technician.Availability.available) {
            eventPublisher.publishEvent(new TechnicianAvailableEvent(this, saved.getId()));
        }

        activityLogService.log(adminId, adminName, "admin",
                "FORCE_ACTIVATE_TECHNICIAN",
                "Force activated technician: " + tech.getName() + " (id=" + id + ") - now available for matching");
        return userMapper.toResponse(saved);
    }

    // ─────────────────────────────────────
    // PAYMENT MANAGEMENT
    // ─────────────────────────────────────

    /**
     * Get pending payments for a specific technician
     */
    public List<TechnicianPayment> getTechnicianPendingPayments(Long technicianId) {
        technicianRepository.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found"));
        
        List<TechnicianPayment> registrationPayments = paymentRepository
                .findByTechnicianIdAndStatusAndPaymentTypeOrderByDueDateAsc(
                        technicianId, 
                        TechnicianPayment.PaymentStatus.PENDING, 
                        TechnicianPayment.PaymentType.REGISTRATION);
        
        List<TechnicianPayment> subscriptionPayments = paymentRepository
                .findByTechnicianIdAndStatusAndPaymentTypeOrderByDueDateAsc(
                        technicianId, 
                        TechnicianPayment.PaymentStatus.PENDING, 
                        TechnicianPayment.PaymentType.MONTHLY_SUBSCRIPTION);
        
        List<TechnicianPayment> all = new ArrayList<>();
        all.addAll(registrationPayments);
        all.addAll(subscriptionPayments);
        return all;
    }

    /**
     * Manually activate a technician after verifying payment outside system
     * (e.g., bank transfer, cash payment, etc.)
     */
    @Transactional
    public UserResponse activateTechnicianAfterPayment(Long id, String paymentMethod, String notes, 
                                                       Long adminId, String adminName) {
        Technician tech = technicianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found with id: " + id));

        // Mark pending registration payment as completed manually
        List<TechnicianPayment> pendingRegistration = paymentRepository
                .findByTechnicianIdAndStatusAndPaymentTypeOrderByDueDateAsc(
                        id, 
                        TechnicianPayment.PaymentStatus.PENDING, 
                        TechnicianPayment.PaymentType.REGISTRATION);

        if (!pendingRegistration.isEmpty()) {
            TechnicianPayment payment = pendingRegistration.get(0);
            payment.setStatus(TechnicianPayment.PaymentStatus.SUCCESS);
            payment.setPaymentMethod(paymentMethod);
            payment.setPaidAt(java.time.LocalDateTime.now());
            paymentRepository.save(payment);
        }

        // Activate technician account
        tech.setAccountStatus(Technician.AccountStatus.ACTIVE);
        tech.setStatus(Technician.ApprovalStatus.approved);
        tech.setSubscriptionExpiryAt(LocalDate.now().plusMonths(1));
        tech.setAvailability(Technician.Availability.available);
        Technician saved = technicianRepository.save(tech);

        eventPublisher.publishEvent(new TechnicianAvailableEvent(this, saved.getId()));

        activityLogService.log(adminId, adminName, "admin",
                "ACTIVATE_AFTER_PAYMENT",
                "Activated technician " + tech.getName() + " after manual payment verification. " +
                "Method: " + paymentMethod + ". Notes: " + notes);

        return userMapper.toResponse(saved);
    }

    /**
     * Get all technicians grouped by payment status
     */
    public Map<String, Object> getAllTechniciansPaymentStatus() {
        List<Technician> allTechs = technicianRepository.findAll();
        
        long pendingPayment = allTechs.stream()
                .filter(t -> t.getAccountStatus() == Technician.AccountStatus.PENDING_PAYMENT)
                .count();
        
        long activeWithValidSubscription = allTechs.stream()
                .filter(t -> t.getAccountStatus() == Technician.AccountStatus.ACTIVE && 
                        (t.getSubscriptionExpiryAt() != null && t.getSubscriptionExpiryAt().isAfter(LocalDate.now())))
                .count();
        
        long inactiveOverduePayment = allTechs.stream()
                .filter(t -> t.getAccountStatus() == Technician.AccountStatus.INACTIVE)
                .count();
        
        long blocked = allTechs.stream()
                .filter(t -> t.getAccountStatus() == Technician.AccountStatus.BLOCKED)
                .count();

        return Map.of(
                "totalTechnicians", allTechs.size(),
                "pendingPayment", pendingPayment,
                "activeWithValidSubscription", activeWithValidSubscription,
                "inactiveOverduePayment", inactiveOverduePayment,
                "blocked", blocked
        );
    }

    public Map<String, Object> getDashboardStats() {
        // Get technician counts by status
        long totalTechnicians = technicianRepository.count();
        long pendingTechnicians = technicianRepository.countByStatus(Technician.ApprovalStatus.pending);
        long approvedTechnicians = technicianRepository.countByStatus(Technician.ApprovalStatus.approved);
        long rejectedTechnicians = technicianRepository.countByStatus(Technician.ApprovalStatus.rejected);

        // Get service request counts
        long totalRequests = serviceRequestRepository.count();
        long searchingRequests = serviceRequestRepository.countByStatus(ServiceRequest.RequestStatus.searching);
        long matchedRequests = serviceRequestRepository.countByStatus(ServiceRequest.RequestStatus.matched);
        long inProgressRequests = serviceRequestRepository.countByStatus(ServiceRequest.RequestStatus.in_progress);
        long completedRequests = serviceRequestRepository.countByStatus(ServiceRequest.RequestStatus.completed);
        long cancelledRequests = serviceRequestRepository.countByStatus(ServiceRequest.RequestStatus.cancelled);

        // Calculate active requests (searching + matched + in_progress)
        long activeRequests = searchingRequests + matchedRequests + inProgressRequests;

        return Map.of(
            "totalTechnicians", totalTechnicians,
            "pendingTechnicians", pendingTechnicians,
            "approvedTechnicians", approvedTechnicians,
            "rejectedTechnicians", rejectedTechnicians,
            "totalRequests", totalRequests,
            "activeRequests", activeRequests,
            "completedRequests", completedRequests,
            "cancelledRequests", cancelledRequests
        );
    }

    public Page<ActivityLogResponse> getLogs(Pageable pageable) {
        return activityLogService.getAllLogs(pageable);
    }

    // Keep the old method for backward compatibility
    public List<ActivityLogResponse> getLogs() {
        return getLogs(Pageable.unpaged()).getContent();
    }
}
