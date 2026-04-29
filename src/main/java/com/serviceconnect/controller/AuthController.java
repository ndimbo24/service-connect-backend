package com.serviceconnect.controller;

import com.serviceconnect.dto.request.LoginRequest;
import com.serviceconnect.dto.request.RegisterRequest;
import com.serviceconnect.dto.response.ApiResponse;
import com.serviceconnect.dto.response.AuthResponse;
import com.serviceconnect.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /auth/login
     * Body: { phone, password, role }
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    /**
     * POST /auth/register
     * Body for client:     { name, phone, password, role: "client" }
     * Body for technician: { name, phone, password, role: "technician", serviceTypes, location }
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(201).body(ApiResponse.success("Registration successful", response));
    }
}
