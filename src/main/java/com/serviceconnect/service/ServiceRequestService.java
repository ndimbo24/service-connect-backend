package com.serviceconnect.service;

import com.serviceconnect.dto.request.CreateServiceRequest;
import com.serviceconnect.dto.response.AiInputAnalysisResponse;
import com.serviceconnect.dto.response.JobNotificationResponse;
import com.serviceconnect.dto.response.ServiceRequestResponse;
import com.serviceconnect.dto.response.TrackingResponse;
import com.serviceconnect.entity.Client;
import com.serviceconnect.entity.JobNotification;
import com.serviceconnect.entity.ServiceRequest;
import com.serviceconnect.entity.Technician;
import com.serviceconnect.entity.User;
import com.serviceconnect.exception.BadRequestException;
import com.serviceconnect.exception.ResourceNotFoundException;
import com.serviceconnect.exception.UnauthorizedException;
import com.serviceconnect.repository.JobNotificationRepository;
import com.serviceconnect.repository.ServiceRequestRepository;
import com.serviceconnect.repository.TechnicianRepository;
import com.serviceconnect.repository.UserRepository;
import com.serviceconnect.util.LocationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceRequestService {

    private final ServiceRequestRepository requestRepository;
    private final TechnicianRepository technicianRepository;
    private final JobNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final AiInputAnalysisService aiInputAnalysisService;

    // ──────────────────────────────────────────────
    // CREATE REQUEST
    // ──────────────────────────────────────────────
    @Transactional
    public ServiceRequestResponse createRequest(Long clientId, CreateServiceRequest dto, String language) {
        log.info("=== CREATING REQUEST for clientId={}, serviceType={}, location=({},{}) ===",
                clientId, dto.getServiceType(), dto.getLocation().getLat(), dto.getLocation().getLng());

        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        AiInputAnalysisResponse analysis = aiInputAnalysisService.analyze(
                dto.getDescription(),
                dto.getVoiceText(),
                dto.getImageData(),
                dto.getServiceType(),
                language);

        log.info("AI Analysis: detectedCategory={}, suggestedTechnicianType={}",
                analysis.getDetectedCategory(), analysis.getSuggestedTechnicianType());

        ServiceRequest req = ServiceRequest.builder()
                .clientId(clientId)
                .clientName(client.getName())
                .serviceType(dto.getServiceType())
                .description(dto.getDescription())
                .voiceText(dto.getVoiceText())
                .imageData(dto.getImageData())
                .aiCategory(analysis.getDetectedCategory())
                .aiSuggestedTechnicianType(analysis.getSuggestedTechnicianType())
                .locationLat(dto.getLocation().getLat())
                .locationLng(dto.getLocation().getLng())
                .locationAddress(dto.getLocation().getAddress())
                .status(ServiceRequest.RequestStatus.pending)
                .build();

        ServiceRequest saved = requestRepository.save(req);
        log.info("Service request saved with id={}", saved.getId());

        activityLogService.log(clientId, client.getName(), "client",
                "CREATE_REQUEST",
                "New service request for: " + dto.getServiceType() + " (id=" + saved.getId() + ")");

        if (client instanceof Client clientEntity) {
            clientEntity.setLocationLat(dto.getLocation().getLat());
            clientEntity.setLocationLng(dto.getLocation().getLng());
            clientEntity.setAddress(dto.getLocation().getAddress());
            userRepository.save(clientEntity);
            log.info("Updated client location: lat={}, lng={}, address={}",
                    dto.getLocation().getLat(), dto.getLocation().getLng(), dto.getLocation().getAddress());
        }

        // Immediately kick off matching (now auto-assigns)
        try {
            triggerMatching(saved);
        } catch (Exception e) {
            log.error("Error during matching for request id={}: {}", saved.getId(), e.getMessage(), e);
            // Update request status to failed so client knows something went wrong
            saved.setStatus(ServiceRequest.RequestStatus.failed);
            requestRepository.save(saved);
            throw e;
        }

        // Fetch the updated request with assignment details
        ServiceRequest updated = requestRepository.findById(saved.getId())
                .orElse(saved);

        log.info("Request id={} final status after matching: {}", updated.getId(), updated.getStatus());

        return toResponse(updated);
    }

    // ──────────────────────────────────────────────
    // RE-MATCHING: When technicians become available,
    // try to assign them to any waiting "searching" requests.
    // ──────────────────────────────────────────────
    @Transactional
    public void reprocessSearchingRequestsForTechnician(Long technicianId) {
        Technician technician = technicianRepository.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found: " + technicianId));

        if (technician.getAvailability() != Technician.Availability.available) {
            log.info("Technician id={} is not available (status={}). Skipping re-matching.",
                    technicianId, technician.getAvailability());
            return;
        }

        List<ServiceRequest> searchingRequests = requestRepository.findByStatus(ServiceRequest.RequestStatus.searching);
        if (searchingRequests.isEmpty()) {
            log.info("No searching requests found. Technician id={} remains available.", technicianId);
            return;
        }

        log.info("Re-matching technician id={} against {} searching requests", technicianId, searchingRequests.size());

        // Find the best matching request for this technician
        ServiceRequest bestMatch = null;
        int bestTier = Integer.MAX_VALUE; // lower = better (1=district, 2=region, 3=gps, 4=global)
        double bestDistance = Double.MAX_VALUE;
        boolean bestSameStreet = false;

        for (ServiceRequest req : searchingRequests) {
            String targetServiceType = (req.getAiSuggestedTechnicianType() != null && !req.getAiSuggestedTechnicianType().isBlank())
                    ? req.getAiSuggestedTechnicianType()
                    : req.getServiceType();

            // Check service type match
            if (!hasServiceType(technician, targetServiceType)) {
                continue;
            }

            // Determine location tier
            LocationMapper.RegionDistrict rd = LocationMapper.toRegionDistrict(req.getLocationLat(), req.getLocationLng());
            String clientRegion = rd.region();
            String clientDistrict = rd.district();

            int tier = Integer.MAX_VALUE;
            double distance = Double.MAX_VALUE;
            boolean sameStreetMatch = isStreetNearRequest(req, technician);

            if (!"Unknown".equalsIgnoreCase(clientRegion) && !"Unknown".equalsIgnoreCase(clientDistrict)
                    && clientRegion.equalsIgnoreCase(technician.getRegion())
                    && clientDistrict.equalsIgnoreCase(technician.getDistrict())) {
                tier = 1; // Same district
            } else if (!"Unknown".equalsIgnoreCase(clientRegion)
                    && clientRegion.equalsIgnoreCase(technician.getRegion())) {
                tier = 2; // Same region
            } else if (technician.getLocationLat() != null && technician.getLocationLng() != null) {
                distance = haversine(req.getLocationLat(), req.getLocationLng(),
                        technician.getLocationLat(), technician.getLocationLng());
                if (distance <= 50.0) {
                    tier = 3; // Within GPS radius
                } else {
                    tier = 4; // Global fallback
                }
            } else {
                tier = 4; // Global fallback (no GPS coords)
            }

            // Prefer: lower tier, same street match, older request (FIFO), then shorter distance
            if (bestMatch == null
                    || tier < bestTier
                    || (tier == bestTier && sameStreetMatch && !bestSameStreet)
                    || (tier == bestTier && sameStreetMatch == bestSameStreet && req.getCreatedAt().isBefore(bestMatch.getCreatedAt()))
                    || (tier == bestTier && sameStreetMatch == bestSameStreet && req.getCreatedAt().equals(bestMatch.getCreatedAt()) && distance < bestDistance)) {
                bestMatch = req;
                bestTier = tier;
                bestDistance = distance;
                bestSameStreet = sameStreetMatch;
            }
        }

        if (bestMatch == null) {
            log.info("Technician id={} does not match any searching request. Remains available.", technicianId);
            return;
        }

        // Auto-assign
        double distance = (technician.getLocationLat() != null && technician.getLocationLng() != null)
                ? haversine(bestMatch.getLocationLat(), bestMatch.getLocationLng(),
                        technician.getLocationLat(), technician.getLocationLng())
                : 0.0;

        bestMatch.setTechnicianId(technician.getId());
        bestMatch.setTechnicianName(technician.getName());
        bestMatch.setStatus(ServiceRequest.RequestStatus.matched);
        bestMatch.setEstimatedArrival(LocalDateTime.now().plusMinutes(
                Math.max(15, (int) Math.ceil((distance / 30.0) * 60))));

        technician.setAvailability(Technician.Availability.busy);
        technician.setTotalJobs(technician.getTotalJobs() != null ? technician.getTotalJobs() + 1 : 1);
        technicianRepository.save(technician);
        requestRepository.save(bestMatch);

        log.info("Re-matched: request id={} -> technician id={}, tier={}, distance={}km",
                bestMatch.getId(), technician.getId(), bestTier, distance);
    }

    /**
     * Process all newly available technicians and match them to waiting requests.
     * This should be called whenever technicians become available (not just after job completion).
     */
    @Transactional
    public int processNewlyAvailableTechnicians() {
        List<Technician> availableTechnicians = technicianRepository.findAvailableTechniciansWithServiceTypes(
                Technician.Availability.available,
                LocalDate.now());
        if (availableTechnicians.isEmpty()) {
            log.debug("No available technicians found for re-matching.");
            return 0;
        }

        List<ServiceRequest> searchingRequests = requestRepository.findByStatus(ServiceRequest.RequestStatus.searching);
        if (searchingRequests.isEmpty()) {
            log.debug("No searching requests found. {} technicians remain available.", availableTechnicians.size());
            return 0;
        }

        log.info("Processing {} available technicians against {} searching requests",
                availableTechnicians.size(), searchingRequests.size());

        // Sort technicians by priority (higher rating first, then more experience)
        availableTechnicians.sort((t1, t2) -> {
            int ratingCompare = Double.compare(
                t2.getRating() != null ? t2.getRating() : 0.0,
                t1.getRating() != null ? t1.getRating() : 0.0
            );
            if (ratingCompare != 0) return ratingCompare;

            return Integer.compare(
                t2.getTotalJobs() != null ? t2.getTotalJobs() : 0,
                t1.getTotalJobs() != null ? t1.getTotalJobs() : 0
            );
        });

        int matchedCount = 0;
        for (Technician technician : availableTechnicians) {
            if (technician.getAvailability() != Technician.Availability.available) {
                continue; // Skip if already assigned
            }

            // Try to match this technician to a waiting request
            ServiceRequest bestMatch = findBestMatchForTechnician(technician, searchingRequests);
            if (bestMatch != null) {
                assignTechnicianToRequest(technician, bestMatch);
                searchingRequests.remove(bestMatch); // Remove from pool to avoid double assignment
                matchedCount++;
                log.info("Real-time matched: technician {} -> request {}", technician.getId(), bestMatch.getId());
            }
        }

        log.info("Real-time matching completed: {} technicians matched to requests", matchedCount);
        return matchedCount;
    }

    /**
     * Find the best matching request for a specific technician from a pool of searching requests.
     */
    private ServiceRequest findBestMatchForTechnician(Technician technician, List<ServiceRequest> searchingRequests) {
        log.debug("Finding best match for technician {} (region={}, district={}, lat={}, lng={}) from {} searching requests",
                technician.getId(), technician.getRegion(), technician.getDistrict(),
                technician.getLocationLat(), technician.getLocationLng(), searchingRequests.size());

        ServiceRequest bestMatch = null;
        int bestTier = Integer.MAX_VALUE;
        double bestDistance = Double.MAX_VALUE;
        boolean bestSameStreet = false;

        for (ServiceRequest req : searchingRequests) {
            String targetServiceType = (req.getAiSuggestedTechnicianType() != null && !req.getAiSuggestedTechnicianType().isBlank())
                    ? req.getAiSuggestedTechnicianType()
                    : req.getServiceType();

            // Check service type match
            if (!hasServiceType(technician, targetServiceType)) {
                continue;
            }

            // Determine location tier
            LocationMapper.RegionDistrict rd = LocationMapper.toRegionDistrict(req.getLocationLat(), req.getLocationLng());
            String clientRegion = rd.region();
            String clientDistrict = rd.district();

            int tier = Integer.MAX_VALUE;
            double distance = Double.MAX_VALUE;
            boolean sameStreetMatch = isStreetNearRequest(req, technician);

            if (!"Unknown".equalsIgnoreCase(clientRegion) && !"Unknown".equalsIgnoreCase(clientDistrict)
                    && clientRegion.equalsIgnoreCase(technician.getRegion())
                    && clientDistrict.equalsIgnoreCase(technician.getDistrict())) {
                tier = 1; // Same district
            } else if (!"Unknown".equalsIgnoreCase(clientRegion)
                    && clientRegion.equalsIgnoreCase(technician.getRegion())) {
                tier = 2; // Same region
            } else if (technician.getLocationLat() != null && technician.getLocationLng() != null) {
                distance = haversine(req.getLocationLat(), req.getLocationLng(),
                        technician.getLocationLat(), technician.getLocationLng());
                if (distance <= 20.0) {  // Reduced from 50km to 20km for better precision
                    tier = 3; // Within GPS radius
                } else {
                    tier = 4; // Global fallback
                }
            } else {
                tier = 4; // Global fallback
            }

            // Prefer: lower tier, same street match, older request (FIFO), then shorter distance
            if (bestMatch == null
                    || tier < bestTier
                    || (tier == bestTier && sameStreetMatch && !bestSameStreet)
                    || (tier == bestTier && sameStreetMatch == bestSameStreet && req.getCreatedAt().isBefore(bestMatch.getCreatedAt()))
                    || (tier == bestTier && sameStreetMatch == bestSameStreet && req.getCreatedAt().equals(bestMatch.getCreatedAt()) && distance < bestDistance)) {
                bestMatch = req;
                bestTier = tier;
                bestDistance = distance;
                bestSameStreet = sameStreetMatch;
            }
        }

        return bestMatch;
    }

    /**
     * Assign a technician to a request and update their status.
     */
    private void assignTechnicianToRequest(Technician technician, ServiceRequest request) {
        double distance = (technician.getLocationLat() != null && technician.getLocationLng() != null)
                ? haversine(request.getLocationLat(), request.getLocationLng(),
                        technician.getLocationLat(), technician.getLocationLng())
                : 0.0;

        request.setTechnicianId(technician.getId());
        request.setTechnicianName(technician.getName());
        request.setStatus(ServiceRequest.RequestStatus.matched);
        request.setEstimatedArrival(LocalDateTime.now().plusMinutes(
                Math.max(15, (int) Math.ceil((distance / 30.0) * 60))));

        technician.setAvailability(Technician.Availability.busy);
        technician.setTotalJobs(technician.getTotalJobs() != null ? technician.getTotalJobs() + 1 : 1);

        technicianRepository.save(technician);
        requestRepository.save(request);
    }

    private boolean hasServiceType(Technician technician, String serviceType) {
        if (technician.getServiceTypes() == null || serviceType == null) return false;
        String target = serviceType.toLowerCase();
        return technician.getServiceTypes().stream()
                .anyMatch(st -> st != null && st.toLowerCase().equals(target));
    }

    private boolean isStreetNearRequest(ServiceRequest req, Technician tech) {
        String requestAddress = normalizeLocationText(req.getLocationAddress());
        String technicianStreet = normalizeLocationText(tech.getStreet());

        if (requestAddress.isBlank() || technicianStreet.isBlank()) {
            return false;
        }

        if (requestAddress.contains(technicianStreet) || technicianStreet.contains(requestAddress)) {
            return true;
        }

        Set<String> requestWords = new java.util.HashSet<>(List.of(requestAddress.split("\\s+")));
        for (String word : technicianStreet.split("\\s+")) {
            if (word.length() >= 4 && requestWords.contains(word)) {
                return true;
            }
        }

        return false;
    }

    private String normalizeLocationText(String text) {
        if (text == null) return "";
        return text.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private double getDistanceKm(ServiceRequest req, Technician tech) {
        if (req.getLocationLat() == null || req.getLocationLng() == null
                || tech.getLocationLat() == null || tech.getLocationLng() == null) {
            return Double.MAX_VALUE;
        }
        return haversine(req.getLocationLat(), req.getLocationLng(), tech.getLocationLat(), tech.getLocationLng());
    }

    // ──────────────────────────────────────────────
    // MATCHING LOGIC — Automatic assignment
    // 4-tier fallback strategy:
    //   1. Same district (exact region+district match)
    //   2. Same region (any district in region)
    //   3. GPS radius fallback (within 50km using legacy lat/lng)
    //   4. Global fallback (any available technician with service type)
    // ──────────────────────────────────────────────
    @Transactional
    public void triggerMatching(ServiceRequest req) {
        log.info("=== STARTING MATCHING for request id={}, serviceType={}, location=({},{}) ===",
                req.getId(), req.getServiceType(), req.getLocationLat(), req.getLocationLng());

        req.setStatus(ServiceRequest.RequestStatus.searching);
        requestRepository.save(req);

        String targetServiceType = (req.getAiSuggestedTechnicianType() != null && !req.getAiSuggestedTechnicianType().isBlank())
                ? req.getAiSuggestedTechnicianType()
                : req.getServiceType();

        log.info("Target service type: {}", targetServiceType);

        // Convert client coordinates to region + district
        LocationMapper.RegionDistrict rd = LocationMapper.toRegionDistrict(
                req.getLocationLat(), req.getLocationLng());

        log.info("Region/District mapping: region={}, district={}", rd.region(), rd.district());

        // Capture region/district as final for later use in lambdas
        final String clientRegion = rd.region();
        final String clientDistrict = rd.district();

        // Must have valid region to proceed
        if ("Unknown".equalsIgnoreCase(clientRegion) || "Unknown".equalsIgnoreCase(clientDistrict)) {
            log.warn("Unknown region/district for request id={} at lat={}, lng={}. " +
                    "Client location outside mapped zones. " +
                    "Will attempt GPS-radius matching only.",
                    req.getId(), req.getLocationLat(), req.getLocationLng());
            // Skip to GPS radius search (tier 3)
            // Keep region/district as null to skip tier 1 & 2
        }

        List<Technician> candidates = new java.util.ArrayList<>();

        // ── Tier 1: Same district exact match ──────────────────────────────────
        if (!"Unknown".equalsIgnoreCase(clientRegion) && !"Unknown".equalsIgnoreCase(clientDistrict)) {
            candidates = technicianRepository.findAvailableByRegionAndDistrict(clientRegion, clientDistrict, LocalDate.now());
            log.info("Tier 1 (same district): {} candidates", candidates.size());

            if (!candidates.isEmpty()) {
                candidates = filterByServiceType(candidates, targetServiceType);
                log.info("After service filter: {} candidates in same district", candidates.size());
            }
        }

        // ── Tier 2: Same region (any district) ─────────────────────────────────
        if (candidates.isEmpty() && !"Unknown".equalsIgnoreCase(clientRegion)) {
            log.info("Tier 1 returned empty. Trying Tier 2: same region, any district");
            candidates = technicianRepository.findAvailableByRegion(clientRegion, LocalDate.now());
            log.info("Tier 2 (same region): {} candidates", candidates.size());

            if (!candidates.isEmpty()) {
                candidates = filterByServiceType(candidates, targetServiceType);
                log.info("After service filter: {} candidates in same region", candidates.size());
            }
        }

        // ── Tier 3: GPS radius fallback (within 20km) ─────────────────────────
        if (candidates.isEmpty()) {
            log.info("Tier 2 returned empty. Trying Tier 3: GPS radius within 20km");
            candidates = technicianRepository.findAvailableByServiceTypeAndLocation(
                    targetServiceType, req.getLocationLat(), req.getLocationLng(), 20.0, LocalDate.now());
            log.info("Tier 3 (GPS radius): {} candidates", candidates.size());
        }

        // ── Tier 4: Global fallback (any available technician with service type) ──
        if (candidates.isEmpty()) {
            log.warn("Tier 3 returned empty. Trying Tier 4: ANY available technician with service type={}", targetServiceType);
            candidates = technicianRepository.findAvailableByServiceTypeOnly(targetServiceType, LocalDate.now());
            log.info("Tier 4 (global): {} candidates", candidates.size());
        }

        if (candidates.isEmpty()) {
            log.warn("No available technicians found for service type: {} after all 4 tiers. Request id={} remains in searching.",
                    targetServiceType, req.getId());
            return;
        }

        log.info("Candidate technicians: {}", candidates.stream()
                .map(t -> String.format("id=%d,name=%s,region=%s,district=%s,lat=%s,lng=%s,serviceTypes=%s",
                        t.getId(), t.getName(), t.getRegion(), t.getDistrict(),
                        t.getLocationLat(), t.getLocationLng(), t.getServiceTypes()))
                .collect(Collectors.toList()));

        // Select best candidate: prioritize same-street / same-address technicians in the same district,
        // then choose the closest available technician.
        List<Technician> sortedCandidates = candidates.stream()
                .sorted(Comparator
                        .comparing((Technician t) -> !isStreetNearRequest(req, t))
                        .thenComparingDouble(t -> getDistanceKm(req, t)))
                .toList();

        Technician best = sortedCandidates.get(0);

        double distance = (best.getLocationLat() != null && best.getLocationLng() != null)
                ? haversine(req.getLocationLat(), req.getLocationLng(), best.getLocationLat(), best.getLocationLng())
                : 0.0;

        log.info("Selected technician: id={}, name={}, distance={}km (tier used: {})",
                best.getId(), best.getName(), distance,
                (!"Unknown".equalsIgnoreCase(clientRegion) && !"Unknown".equalsIgnoreCase(clientDistrict) &&
                 best.getRegion().equalsIgnoreCase(clientRegion) && best.getDistrict().equalsIgnoreCase(clientDistrict))
                        ? "Tier1-same-district"
                        : (!"Unknown".equalsIgnoreCase(clientRegion) && best.getRegion().equalsIgnoreCase(clientRegion))
                                ? "Tier2-same-region"
                                : (distance > 0 && distance <= 50.0) ? "Tier3-GPS-radius" : "Tier4-global");

        // Auto-assign
        req.setTechnicianId(best.getId());
        req.setTechnicianName(best.getName());
        req.setStatus(ServiceRequest.RequestStatus.matched);
        req.setEstimatedArrival(LocalDateTime.now().plusMinutes(
                Math.max(15, (int) Math.ceil((distance / 30.0) * 60))));

        best.setAvailability(Technician.Availability.busy);
        best.setTotalJobs(best.getTotalJobs() != null ? best.getTotalJobs() + 1 : 1);
        technicianRepository.save(best);

        requestRepository.save(req);

        log.info("Request id={} auto-assigned to technician id={}, distance={}km, status={}, tier={}",
                req.getId(), best.getId(), distance, req.getStatus(),
                (!"Unknown".equalsIgnoreCase(clientRegion) && !"Unknown".equalsIgnoreCase(clientDistrict) &&
                 best.getRegion().equalsIgnoreCase(clientRegion) && best.getDistrict().equalsIgnoreCase(clientDistrict))
                        ? "Tier1-exact-district"
                        : (!"Unknown".equalsIgnoreCase(clientRegion) && best.getRegion().equalsIgnoreCase(clientRegion))
                                ? "Tier2-same-region"
                                : (distance > 0 && distance <= 50.0) ? "Tier3-GPS-radius" : "Tier4-global");
    }

    // Helper: case-insensitive service type filter
    private List<Technician> filterByServiceType(List<Technician> technicians, String serviceType) {
        String target = serviceType.toLowerCase();
        return technicians.stream()
                .filter(t -> t.getServiceTypes() != null &&
                             t.getServiceTypes().stream()
                                 .anyMatch(st -> st != null && st.toLowerCase().equals(target)))
                .collect(Collectors.toList());
    }

    /**
     * Calculate distance between two GPS coordinates using Haversine formula.
     * @return distance in kilometers
     */
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // ──────────────────────────────────────────────
    // ASSIGN TECHNICIAN
    // ──────────────────────────────────────────────
    @Transactional
    public ServiceRequestResponse assignTechnician(Long requestId, Long technicianId) {
        ServiceRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found: " + requestId));
        Technician tech = technicianRepository.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found: " + technicianId));
        if (tech.getStatus() != Technician.ApprovalStatus.approved
                || tech.getAccountStatus() != Technician.AccountStatus.ACTIVE) {
            throw new BadRequestException("Technician is not eligible to receive requests");
        }

        req.setTechnicianId(technicianId);
        req.setTechnicianName(tech.getName());
        req.setStatus(ServiceRequest.RequestStatus.matched);

        // Calculate ETA based on distance (assume average speed of 30 km/h)
        double distance = haversine(req.getLocationLat(), req.getLocationLng(),
                                   tech.getLocationLat(), tech.getLocationLng());
        int etaMinutes = Math.max(15, (int) Math.ceil((distance / 30.0) * 60)); // at least 15 minutes
        req.setEstimatedArrival(LocalDateTime.now().plusMinutes(etaMinutes));

        tech.setAvailability(Technician.Availability.busy);
        technicianRepository.save(tech);

        // Update notification
        notificationRepository.findByRequestId(requestId).forEach(n -> {
            if (n.getTechnicianId().equals(technicianId)) {
                n.setStatus(JobNotification.NotificationStatus.accepted);
                notificationRepository.save(n);
            }
        });

        activityLogService.log(technicianId, tech.getName(), "technician",
                "ASSIGN_REQUEST",
                "Technician assigned to request id=" + requestId);

        return toResponse(requestRepository.save(req));
    }

    // ──────────────────────────────────────────────
    // GET REQUESTS
    // ──────────────────────────────────────────────
    public ServiceRequestResponse getById(Long id) {
        ServiceRequest req = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found: " + id));
        return toResponse(req);
    }

    public List<ServiceRequestResponse> getByClientId(Long clientId) {
        return requestRepository.findByClientIdOrderByCreatedAtDesc(clientId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ServiceRequestResponse> getByTechnicianId(Long technicianId) {
        return requestRepository.findByTechnicianIdOrderByCreatedAtDesc(technicianId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public Page<ServiceRequestResponse> getAll(Pageable pageable) {
        return requestRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toResponse);
    }

    // Keep the old method for backward compatibility
    public List<ServiceRequestResponse> getAll() {
        return getAll(Pageable.unpaged()).getContent();
    }

    // ──────────────────────────────────────────────
    // STATUS TRANSITIONS
    // ──────────────────────────────────────────────
    @Transactional
    public ServiceRequestResponse updateStatus(Long requestId, String newStatus, Long actorId) {
        ServiceRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found: " + requestId));

        ServiceRequest.RequestStatus status;
        try {
            status = ServiceRequest.RequestStatus.valueOf(newStatus);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + newStatus);
        }

        req.setStatus(status);
        if (status == ServiceRequest.RequestStatus.completed) {
            req.setCompletedAt(LocalDateTime.now());
            // Free up technician and trigger re-matching for searching requests
            if (req.getTechnicianId() != null) {
                technicianRepository.findById(req.getTechnicianId()).ifPresent(t -> {
                    t.setAvailability(Technician.Availability.available);
                    t.setTotalJobs(t.getTotalJobs() + 1);
                    technicianRepository.save(t);
                    // Try to match this newly available technician to waiting requests
                    reprocessSearchingRequestsForTechnician(t.getId());
                });
            }
        }

        return toResponse(requestRepository.save(req));
    }

    @Transactional
    public ServiceRequestResponse cancelRequest(Long requestId, Long actorId) {
        ServiceRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found: " + requestId));

        if (!req.getClientId().equals(actorId)) {
            throw new UnauthorizedException("Only the client who created this request can cancel it");
        }
        if (req.getStatus() == ServiceRequest.RequestStatus.completed) {
            throw new BadRequestException("Cannot cancel a completed request");
        }

        // Free up technician if already assigned
        if (req.getTechnicianId() != null) {
            technicianRepository.findById(req.getTechnicianId()).ifPresent(t -> {
                t.setAvailability(Technician.Availability.available);
                technicianRepository.save(t);
                // Try to match this newly available technician to waiting requests
                reprocessSearchingRequestsForTechnician(t.getId());
            });
        }

        req.setStatus(ServiceRequest.RequestStatus.cancelled);
        return toResponse(requestRepository.save(req));
    }

    // ──────────────────────────────────────────────
    // TRACKING
    // ──────────────────────────────────────────────
    public TrackingResponse getTracking(Long requestId) {
        ServiceRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found: " + requestId));

        TrackingResponse.TrackingResponseBuilder builder = TrackingResponse.builder()
                .requestId(requestId)
                .requestStatus(req.getStatus().name())
                .estimatedArrival(req.getEstimatedArrival())
                .clientLocation(TrackingResponse.ClientLocation.builder()
                        .lat(req.getLocationLat())
                        .lng(req.getLocationLng())
                        .address(req.getLocationAddress())
                        .build());

        if (req.getTechnicianId() != null) {
            technicianRepository.findById(req.getTechnicianId()).ifPresent(tech -> {
                builder.technicianLocation(TrackingResponse.TechnicianLocation.builder()
                        .technicianId(tech.getId())
                        .technicianName(tech.getName())
                        .lat(tech.getLocationLat())
                        .lng(tech.getLocationLng())
                        .address(tech.getLocationAddress())
                        .availability(tech.getAvailability().name())
                        .build());
            });
        }

        return builder.build();
    }

    // ──────────────────────────────────────────────
    // JOB NOTIFICATIONS (for technician dashboard)
    // ──────────────────────────────────────────────
    public List<JobNotificationResponse> getPendingNotifications(Long technicianId) {
        return notificationRepository
                .findByTechnicianIdAndStatus(technicianId, JobNotification.NotificationStatus.pending)
                .stream()
                .filter(n -> n.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(this::toNotificationResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ServiceRequestResponse acceptJobNotification(Long notificationId, Long technicianId) {
        JobNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        if (!notification.getTechnicianId().equals(technicianId)) {
            throw new UnauthorizedException("You can only accept your own notifications");
        }

        if (notification.getStatus() != JobNotification.NotificationStatus.pending) {
            throw new BadRequestException("Notification is no longer pending");
        }

        ServiceRequest req = requestRepository.findById(notification.getRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        // Assign the technician who accepted
        notification.setStatus(JobNotification.NotificationStatus.accepted);
        notificationRepository.save(notification);

        return assignTechnician(req.getId(), technicianId);
    }

    @Transactional
    public void rejectJobNotification(Long notificationId, Long technicianId) {
        JobNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        if (!notification.getTechnicianId().equals(technicianId)) {
            throw new UnauthorizedException("You can only reject your own notifications");
        }

        if (notification.getStatus() != JobNotification.NotificationStatus.pending) {
            throw new BadRequestException("Notification is no longer pending");
        }

        notification.setStatus(JobNotification.NotificationStatus.rejected);
        notificationRepository.save(notification);

        log.info("Technician id={} rejected notification id={} for request id={}",
                technicianId, notificationId, notification.getRequestId());
    }

    // ──────────────────────────────────────────────
    // HELPERS
    // ──────────────────────────────────────────────
    private ServiceRequestResponse toResponse(ServiceRequest req) {
        ServiceRequestResponse.ServiceRequestResponseBuilder builder = ServiceRequestResponse.builder()
                .id(req.getId())
                .clientId(req.getClientId())
                .clientName(req.getClientName())
                .technicianId(req.getTechnicianId())
                .technicianName(req.getTechnicianName())
                .serviceType(req.getServiceType())
                .description(req.getDescription())
                .voiceText(req.getVoiceText())
                .imageData(req.getImageData())
                .aiCategory(req.getAiCategory())
                .aiSuggestedTechnicianType(req.getAiSuggestedTechnicianType())
                .status(req.getStatus().name())
                .location(ServiceRequestResponse.LocationData.builder()
                        .lat(req.getLocationLat())
                        .lng(req.getLocationLng())
                        .address(req.getLocationAddress())
                        .build())
                .createdAt(req.getCreatedAt())
                .completedAt(req.getCompletedAt())
                .estimatedArrival(req.getEstimatedArrival());

        // Enrich with technician details when matched
        if (req.getTechnicianId() != null) {
            technicianRepository.findById(req.getTechnicianId()).ifPresent(tech -> {
                ServiceRequestResponse.TechnicianInfo info = ServiceRequestResponse.TechnicianInfo.builder()
                        .id(tech.getId())
                        .name(tech.getName())
                        .phone(tech.getPhone())
                        .avatar(tech.getAvatar())
                        .rating(tech.getRating())
                        .totalJobs(tech.getTotalJobs())
                        .serviceTypes(tech.getServiceTypes())
                        .availability(tech.getAvailability().name())
                        .location(tech.getLocationLat() != null
                                ? ServiceRequestResponse.LocationData.builder()
                                    .lat(tech.getLocationLat())
                                    .lng(tech.getLocationLng())
                                    .address(tech.getLocationAddress())
                                    .build()
                                : null)
                        .build();
                builder.technician(info);
            });
        }

        return builder.build();
    }

    private JobNotificationResponse toNotificationResponse(JobNotification n) {
        return JobNotificationResponse.builder()
                .id(n.getId())
                .requestId(n.getRequestId())
                .serviceType(n.getServiceType())
                .clientName(n.getClientName())
                .distance(n.getDistance())
                .address(n.getAddress())
                .description(n.getDescription())
                .expiresAt(n.getExpiresAt())
                .status(n.getStatus().name())
                .build();
    }
}
