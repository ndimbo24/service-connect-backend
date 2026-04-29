package com.serviceconnect.config;

import com.serviceconnect.entity.User;
import com.serviceconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedAdminAccount();
    }

    private void seedAdminAccount() {
        String adminPhone = "1111111111";
        if (!userRepository.existsByPhone(adminPhone)) {
            User admin = new User();
            admin.setName("System Admin");
            admin.setPhone(adminPhone);
            admin.setEmail("admin@serviceconnect.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(User.Role.admin);
            userRepository.save(admin);
            log.info("Admin account seeded: phone={}, password=admin123", adminPhone);
        } else {
            log.info("Admin account already exists, skipping seed.");
        }
    }
}
