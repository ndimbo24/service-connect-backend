package com.serviceconnect.service;

import com.serviceconnect.entity.Technician;
import com.serviceconnect.repository.TechnicianRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionSchedulerService {

    private final TechnicianRepository technicianRepository;

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void deactivateExpiredSubscriptions() {
        List<Technician> expired = technicianRepository.findByAccountStatusAndSubscriptionExpiryAtBefore(
                Technician.AccountStatus.ACTIVE,
                LocalDate.now());

        if (expired.isEmpty()) {
            return;
        }

        for (Technician technician : expired) {
            technician.setAccountStatus(Technician.AccountStatus.SUSPENDED);
            technician.setAvailability(Technician.Availability.offline);
        }
        technicianRepository.saveAll(expired);
        log.info("Suspended {} technicians due to expired subscriptions", expired.size());
    }
}
