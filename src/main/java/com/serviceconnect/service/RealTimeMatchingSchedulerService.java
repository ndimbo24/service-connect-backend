package com.serviceconnect.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealTimeMatchingSchedulerService {

    private final ServiceRequestService serviceRequestService;

    /**
     * Run real-time matching every 30 seconds to catch any technicians
     * that became available through other means (login, subscription renewal, etc.)
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void processRealTimeMatching() {
        try {
            log.debug("Running scheduled real-time matching check");
            int matched = serviceRequestService.processNewlyAvailableTechnicians();
            if (matched > 0) {
                log.info("Scheduled matching completed: {} technicians matched to requests", matched);
            }
        } catch (Exception e) {
            log.error("Error in scheduled real-time matching: {}", e.getMessage(), e);
        }
    }

    /**
     * More frequent check every 10 minutes during peak hours (8 AM - 8 PM)
     * when more requests are expected
     */
    @Scheduled(cron = "0 */10 8-19 * * *") // Every 10 minutes from 8 AM to 8 PM
    public void processPeakHourMatching() {
        try {
            log.debug("Running peak hour real-time matching check");
            int matched = serviceRequestService.processNewlyAvailableTechnicians();
            if (matched > 0) {
                log.info("Peak hour matching completed: {} technicians matched to requests", matched);
            }
        } catch (Exception e) {
            log.error("Error in peak hour real-time matching: {}", e.getMessage(), e);
        }
    }
}