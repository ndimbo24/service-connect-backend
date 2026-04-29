package com.serviceconnect.service;

import com.serviceconnect.entity.Technician;
import com.serviceconnect.exception.ResourceNotFoundException;
import com.serviceconnect.repository.TechnicianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TechnicianPaymentService {

    private final TechnicianRepository technicianRepository;
    private final ActivityLogService activityLogService;

    @Transactional
    public Technician processIpaSubscriptionPayment(Long technicianId, Long actorId, String actorName) {
        Technician technician = technicianRepository.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found: " + technicianId));

        technician.setAccountStatus(Technician.AccountStatus.ACTIVE);
        technician.setSubscriptionExpiryAt(LocalDate.now().plusMonths(1));
        if (technician.getAvailability() == Technician.Availability.offline) {
            technician.setAvailability(Technician.Availability.available);
        }

        Technician saved = technicianRepository.save(technician);
        activityLogService.log(actorId, actorName, "technician",
                "IPA_PAYMENT_SUCCESS",
                "Subscription activated for technician id=" + technicianId);
        return saved;
    }
}
