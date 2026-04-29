package com.serviceconnect.event;

import com.serviceconnect.service.ServiceRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listens for {@link TechnicianAvailableEvent} and triggers real-time matching.
 * <p>
 * This runs asynchronously so that the transaction that made the technician
 * available can commit before matching begins.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TechnicianAvailabilityEventListener {

    private final ServiceRequestService serviceRequestService;

    @Async
    @EventListener
    public void onTechnicianAvailable(TechnicianAvailableEvent event) {
        Long technicianId = event.getTechnicianId();
        log.info("Received TechnicianAvailableEvent for technicianId={}", technicianId);

        try {
            // First try to match this specific technician to waiting requests
            serviceRequestService.reprocessSearchingRequestsForTechnician(technicianId);

            // Then run a broader scan in case multiple technicians became available
            int matched = serviceRequestService.processNewlyAvailableTechnicians();
            if (matched > 0) {
                log.info("Event-driven matching completed: {} technicians matched to requests", matched);
            }
        } catch (Exception e) {
            log.error("Error during event-driven matching for technician {}: {}", technicianId, e.getMessage(), e);
        }
    }
}

