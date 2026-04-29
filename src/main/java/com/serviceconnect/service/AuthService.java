package com.serviceconnect.service;

import com.serviceconnect.dto.request.LoginRequest;
import com.serviceconnect.dto.request.RegisterRequest;
import com.serviceconnect.dto.response.AuthResponse;
import com.serviceconnect.dto.response.UserResponse;
import com.serviceconnect.entity.Client;
import com.serviceconnect.entity.Technician;
import com.serviceconnect.entity.User;
import com.serviceconnect.exception.BadRequestException;
import com.serviceconnect.exception.UnauthorizedException;
import com.serviceconnect.repository.UserRepository;
import com.serviceconnect.security.JwtUtil;
import com.serviceconnect.security.UserPrincipal;
import com.serviceconnect.util.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final ActivityLogService activityLogService;
    private final UserMapper userMapper;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Authenticate credentials
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getPhone(), request.getPassword())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = principal.getUser();

        // Role validation: admin can login as "technician" or "admin"
        String requestedRole = request.getRole();
        String actualRole    = user.getRole().name();

        boolean roleMatch = actualRole.equals(requestedRole)
                || (actualRole.equals("admin") && requestedRole.equals("technician"));

        if (!roleMatch) {
            throw new UnauthorizedException("Account role does not match the selected role");
        }

        String token = jwtUtil.generateToken(principal, user.getId(), actualRole);

        activityLogService.log(user.getId(), user.getName(), actualRole,
                "LOGIN", "User logged in via phone: " + user.getPhone());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(userMapper.toResponse(user))
                .build();
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException("Phone number already registered");
        }

        User saved;
        String role = request.getRole();

        if ("client".equals(role)) {
            Client client = new Client();
            client.setName(request.getName());
            client.setPhone(request.getPhone());
            client.setEmail(request.getEmail());
            client.setPassword(passwordEncoder.encode(request.getPassword()));
            client.setRole(User.Role.client);
            saved = userRepository.save(client);

        } else if ("technician".equals(role)) {
            if (request.getServiceTypes() == null || request.getServiceTypes().isEmpty()) {
                throw new BadRequestException("Service types are required for technician registration");
            }

            Technician technician = new Technician();
            technician.setName(request.getName());
            technician.setPhone(request.getPhone());
            technician.setEmail(request.getEmail());
            technician.setPassword(passwordEncoder.encode(request.getPassword()));
            technician.setRole(User.Role.technician);
            technician.setServiceTypes(request.getServiceTypes());
            technician.setStatus(Technician.ApprovalStatus.pending);
            technician.setAvailability(Technician.Availability.offline);
            technician.setAccountStatus(Technician.AccountStatus.PENDING_PAYMENT);

            if (request.getLocation() != null) {
                // For technicians, location contains region, district, street (structured address)
                technician.setRegion(request.getLocation().getRegion());
                technician.setDistrict(request.getLocation().getDistrict());
                technician.setStreet(request.getLocation().getStreet());
            }

            saved = userRepository.save(technician);
        } else {
            throw new BadRequestException("Invalid role. Must be 'client' or 'technician'");
        }

        activityLogService.log(saved.getId(), saved.getName(), role,
                "REGISTER", "New " + role + " registered: " + saved.getPhone());

        // Issue token
        UserPrincipal principal = new UserPrincipal(saved);
        String token = jwtUtil.generateToken(principal, saved.getId(), role);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(userMapper.toResponse(saved))
                .build();
    }
}
