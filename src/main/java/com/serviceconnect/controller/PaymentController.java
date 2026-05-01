package com.serviceconnect.controller;

import com.serviceconnect.config.PesapalProperties;
import com.serviceconnect.dto.PesapalApiDtos;
import com.serviceconnect.dto.request.InitiatePaymentRequest;
import com.serviceconnect.dto.response.ApiResponse;
import com.serviceconnect.dto.response.InitiatePaymentResponse;
import com.serviceconnect.entity.TechnicianPayment;
import com.serviceconnect.repository.TechnicianPaymentRepository;
import com.serviceconnect.repository.UserRepository;
import com.serviceconnect.security.UserPrincipal;
import com.serviceconnect.service.PesapalService;
import com.serviceconnect.service.PaymentGatewayService;
import com.serviceconnect.util.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentGatewayService paymentGatewayService;
    private final PesapalService pesapalService;
    private final TechnicianPaymentRepository paymentRepository;
    private final PesapalProperties pesapalProperties;
    private final UserMapper userMapper;
    private final UserRepository userRepository;

    // ── 1. Initiate Payment ──────────────────────────────────────────
    @PostMapping("/initiate")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<ApiResponse<InitiatePaymentResponse>> initiatePayment(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody InitiatePaymentRequest request) {
        InitiatePaymentResponse response = paymentGatewayService.initiatePayment(
                request,
                principal.getUser().getEmail(),
                principal.getUser().getPhone(),
                principal.getUser().getName());
        return ResponseEntity.ok(ApiResponse.success("Payment initiated", response));
    }

    // ── 2. Register IPN (Admin only - call once to get IPN ID) ───────
    @PostMapping("/register-ipn")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> registerIpn() {
        String ipnId = pesapalService.registerIpn();
        return ResponseEntity.ok(Map.of(
            "ipn_id", ipnId,
            "message", "Copy this IPN ID and add it to Railway as PESAPAL_IPN_ID"
        ));
    }

    // ── 3. Webhook (PesaPal calls this automatically) ────────────────
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody PesapalApiDtos.IpnNotification payload,
            HttpServletRequest request) {
        log.info("Pesapal webhook received from {}: {}", request.getRemoteAddr(), payload);

        String orderTrackingId = payload.getOrderTrackingId();
        if (orderTrackingId == null || orderTrackingId.isBlank()) {
            log.warn("Invalid webhook payload: missing orderTrackingId");
            return ResponseEntity.ok("Invalid payload");
        }

        // Idempotency check
        Optional<TechnicianPayment> optionalPayment = paymentRepository.findByOrderTrackingId(orderTrackingId);
        if (optionalPayment.isEmpty()) {
            log.warn("Webhook: Payment not found for orderTrackingId: {}", orderTrackingId);
            return ResponseEntity.ok("Payment record not found");
        }

        TechnicianPayment payment = optionalPayment.get();
        if (payment.getStatus() == TechnicianPayment.PaymentStatus.SUCCESS) {
            log.info("Webhook: Payment already processed for orderTrackingId: {}", orderTrackingId);
            return ResponseEntity.ok("Already processed");
        }

        try {
            // Query payment status from PesaPal
            PesapalApiDtos.QueryPaymentStatusResponse statusResponse =
                pesapalService.queryPaymentStatus(orderTrackingId);

            if (statusResponse == null || !"200".equals(statusResponse.getStatus())) {
                log.warn("Webhook: PesaPal API query failed for orderTrackingId: {}", orderTrackingId);
                return ResponseEntity.ok("Query failed");
            }

            String paymentStatusCode = statusResponse.getPaymentStatusCode();
            if (!"1".equals(paymentStatusCode)) { // 1 = Completed
                log.info("Webhook: Payment not completed, status code: {} for orderTrackingId: {}",
                    paymentStatusCode, orderTrackingId);
                return ResponseEntity.ok("Payment not completed");
            }

            // Process successful payment
            paymentGatewayService.processSuccessfulPayment(orderTrackingId);
            payment.setTransactionId(statusResponse.getConfirmationCode());
            payment.setPaymentMethod(statusResponse.getPaymentMethod());
            paymentRepository.save(payment);

            log.info("Webhook: Payment processed successfully for orderTrackingId: {}", orderTrackingId);
            return ResponseEntity.ok("Payment processed");

        } catch (Exception e) {
            log.error("Webhook processing error for orderTrackingId: {}", orderTrackingId, e);
            return ResponseEntity.ok("Webhook received");
        }
    }
}