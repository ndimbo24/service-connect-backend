package com.serviceconnect.service;

import com.serviceconnect.dto.request.UpdateUserRequest;
import com.serviceconnect.dto.response.UserResponse;
import com.serviceconnect.entity.Client;
import com.serviceconnect.entity.Technician;
import com.serviceconnect.entity.User;
import com.serviceconnect.event.TechnicianAvailableEvent;
import com.serviceconnect.exception.BadRequestException;
import com.serviceconnect.exception.ResourceNotFoundException;
import com.serviceconnect.repository.UserRepository;
import com.serviceconnect.util.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;

    public UserResponse getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateMe(Long userId, UpdateUserRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (req.getName() != null)   user.setName(req.getName());
        if (req.getEmail() != null)  user.setEmail(req.getEmail());
        if (req.getAvatar() != null) user.setAvatar(req.getAvatar());

        if (user instanceof Client client) {
            if (req.getAddress() != null) client.setAddress(req.getAddress());
            if (req.getLocation() != null) {
                client.setLocationLat(req.getLocation().getLat());
                client.setLocationLng(req.getLocation().getLng());
            }
        }

        if (user instanceof Technician tech) {
            if (tech.getAccountStatus() == Technician.AccountStatus.ACTIVE
                    && tech.getSubscriptionExpiryAt() != null
                    && tech.getSubscriptionExpiryAt().isBefore(LocalDate.now())) {
                tech.setAccountStatus(Technician.AccountStatus.SUSPENDED);
                tech.setAvailability(Technician.Availability.offline);
                throw new BadRequestException("Technician subscription has expired");
            }

            if (req.getServiceTypes() != null) tech.setServiceTypes(req.getServiceTypes());
            if (req.getAvailability() != null) {
                Technician.Availability newAvailability = Technician.Availability.valueOf(req.getAvailability());
                Technician.Availability oldAvailability = tech.getAvailability();

                if (tech.getStatus() != Technician.ApprovalStatus.approved) {
                    throw new BadRequestException("Technician must be approved before changing availability");
                }
                if (tech.getAccountStatus() != Technician.AccountStatus.ACTIVE) {
                    throw new BadRequestException("Technician subscription is not active");
                }

                tech.setAvailability(newAvailability);

                // If technician is becoming available, trigger re-matching for waiting requests
                if (newAvailability == Technician.Availability.available &&
                    oldAvailability != Technician.Availability.available) {
                    log.info("Technician {} is now available, publishing availability event", tech.getId());
                    eventPublisher.publishEvent(new TechnicianAvailableEvent(this, tech.getId()));
                }
            }
            if (req.getLocation() != null) {
                tech.setLocationLat(req.getLocation().getLat());
                tech.setLocationLng(req.getLocation().getLng());
                tech.setLocationAddress(req.getLocation().getAddress());
            }
        }

        User saved = userRepository.save(user);
        return userMapper.toResponse(saved);
    }
}
