package com.serviceconnect.controller;

import com.serviceconnect.dto.response.ActivityLogResponse;
import com.serviceconnect.dto.response.ApiResponse;
import com.serviceconnect.dto.response.ServiceRequestResponse;
import com.serviceconnect.dto.response.UserResponse;
import com.serviceconnect.security.UserPrincipal;
import com.serviceconnect.service.AdminService;
import com.serviceconnect.service.I18nService;
import com.serviceconnect.service.ServiceRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final ServiceRequestService requestService;
    private final I18nService i18nService;

    // ─────────────────────────────────────
    // TECHNICIAN MANAGEMENT
    // ─────────────────────────────────────

    /**
     * GET /admin/technicians?status=pending|approved|rejected&page=0&size=20
     */
    @GetMapping("/technicians")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getTechnicians(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<UserResponse> technicians = adminService.getTechnicians(status, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(technicians));
    }

    /**
     * GET /admin/technicians/:id
     */
    @GetMapping("/technicians/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getTechnicianById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getTechnicianById(id)));
    }

    /**
     * POST /admin/technicians/:id/approve
     */
    @PostMapping("/technicians/{id}/approve")
    public ResponseEntity<ApiResponse<UserResponse>> approveTechnician(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        UserResponse updated = adminService.approveTechnician(id, principal.getId(), principal.getUser().getName());
        return ResponseEntity.ok(ApiResponse.success(i18nService.msg("admin.technician.approved"), updated));
    }

    /**
     * POST /admin/technicians/:id/reject
     * Body: { reason: "..." }
     */
    @PostMapping("/technicians/{id}/reject")
    public ResponseEntity<ApiResponse<UserResponse>> rejectTechnician(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        String reason = (body != null) ? body.getOrDefault("reason", "No reason provided") : "No reason provided";
        UserResponse updated = adminService.rejectTechnician(id, reason, principal.getId(), principal.getUser().getName());
        return ResponseEntity.ok(ApiResponse.success(i18nService.msg("admin.technician.rejected"), updated));
    }

    @PostMapping("/technicians/{id}/block")
    public ResponseEntity<ApiResponse<UserResponse>> blockTechnician(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        UserResponse updated = adminService.blockTechnician(id, principal.getId(), principal.getUser().getName());
        return ResponseEntity.ok(ApiResponse.success(i18nService.msg("admin.technician.blocked"), updated));
    }

    @PostMapping("/technicians/{id}/unblock")
    public ResponseEntity<ApiResponse<UserResponse>> unblockTechnician(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        UserResponse updated = adminService.unblockTechnician(id, principal.getId(), principal.getUser().getName());
        return ResponseEntity.ok(ApiResponse.success(i18nService.msg("admin.technician.unblocked"), updated));
    }

    @PostMapping("/technicians/{id}/force-activate")
    public ResponseEntity<ApiResponse<UserResponse>> forceActivateTechnician(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        UserResponse updated = adminService.forceActivateTechnician(id, principal.getId(), principal.getUser().getName());
        return ResponseEntity.ok(ApiResponse.success(i18nService.msg("admin.technician.force_activated"), updated));
    }

    // ─────────────────────────────────────
    // TECHNICIAN PAYMENT MANAGEMENT
    // ─────────────────────────────────────

    /**
     * GET /admin/technicians/:id/pending-payments
     * View pending payments for a technician
     */
    @GetMapping("/technicians/{id}/pending-payments")
    public ResponseEntity<ApiResponse<List<?>>> getTechnicianPendingPayments(@PathVariable Long id) {
        var payments = adminService.getTechnicianPendingPayments(id);
        return ResponseEntity.ok(ApiResponse.success("Pending payments", payments));
    }

    /**
     * POST /admin/technicians/:id/activate-after-payment
     * Manually activate technician after they complete payment outside the system
     */
    @PostMapping("/technicians/{id}/activate-after-payment")
    public ResponseEntity<ApiResponse<UserResponse>> activateTechnicianAfterPayment(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        String paymentMethod = (body != null) ? body.getOrDefault("paymentMethod", "MANUAL") : "MANUAL";
        String notes = (body != null) ? body.getOrDefault("notes", "") : "";
        
        UserResponse updated = adminService.activateTechnicianAfterPayment(
                id, paymentMethod, notes, principal.getId(), principal.getUser().getName());
        return ResponseEntity.ok(ApiResponse.success(
                i18nService.msg("admin.technician.activated_after_payment"), updated));
    }

    /**
     * GET /admin/technicians/payment-status/all
     * Get all technicians grouped by payment status
     */
    @GetMapping("/technicians/payment-status/all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTechniciansPaymentStatus() {
        Map<String, Object> paymentStatus = adminService.getAllTechniciansPaymentStatus();
        return ResponseEntity.ok(ApiResponse.success("Technician payment status", paymentStatus));
    }

    // ─────────────────────────────────────
    // SERVICE REQUESTS (admin view)
    // ─────────────────────────────────────

    /**
     * GET /admin/requests?page=0&size=20  — all requests with pagination
     */
    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<Page<ServiceRequestResponse>>> getAllRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ServiceRequestResponse> requests = requestService.getAll(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    // ─────────────────────────────────────
    // ACTIVITY LOGS
    // ─────────────────────────────────────

    /**
     * GET /admin/logs?page=0&size=50
     */
    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<Page<ActivityLogResponse>>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<ActivityLogResponse> logs = adminService.getLogs(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    // ─────────────────────────────────────
    // DASHBOARD STATS
    // ─────────────────────────────────────

    /**
     * GET /admin/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        Map<String, Object> stats = adminService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
