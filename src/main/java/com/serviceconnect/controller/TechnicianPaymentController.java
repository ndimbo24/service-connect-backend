package com.serviceconnect.controller;

import com.serviceconnect.dto.response.ApiResponse;
import com.serviceconnect.dto.response.UserResponse;
import com.serviceconnect.security.UserPrincipal;
import com.serviceconnect.service.I18nService;
import com.serviceconnect.service.TechnicianPaymentService;
import com.serviceconnect.util.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments/technicians")
@RequiredArgsConstructor
public class TechnicianPaymentController {

    private final TechnicianPaymentService technicianPaymentService;
    private final UserMapper userMapper;
    private final I18nService i18nService;

    @PostMapping("/me/ipa/subscribe")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<ApiResponse<UserResponse>> subscribeViaIpa(
            @AuthenticationPrincipal UserPrincipal principal) {
        UserResponse updated = userMapper.toResponse(
                technicianPaymentService.processIpaSubscriptionPayment(
                        principal.getId(), principal.getId(), principal.getUser().getName()));

        return ResponseEntity.ok(ApiResponse.success(i18nService.msg("payment.ipa.success"), updated));
    }
}
