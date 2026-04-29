package com.serviceconnect.controller;

import com.serviceconnect.dto.request.UpdateUserRequest;
import com.serviceconnect.dto.response.ApiResponse;
import com.serviceconnect.dto.response.UserResponse;
import com.serviceconnect.security.UserPrincipal;
import com.serviceconnect.service.I18nService;
import com.serviceconnect.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final I18nService i18nService;

    /**
     * GET /users/me
     * Returns the authenticated user's profile.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(
            @AuthenticationPrincipal UserPrincipal principal) {
        UserResponse user = userService.getById(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * PUT /users/me
     * Update the authenticated user's profile.
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UpdateUserRequest request) {
        UserResponse updated = userService.updateMe(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(i18nService.msg("user.profile.updated"), updated));
    }
}
