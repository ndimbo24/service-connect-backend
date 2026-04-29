package com.serviceconnect.controller;

import com.serviceconnect.dto.response.ApiResponse;
import com.serviceconnect.dto.response.UserResponse;
import com.serviceconnect.entity.Technician;
import com.serviceconnect.exception.ResourceNotFoundException;
import com.serviceconnect.repository.TechnicianRepository;
import com.serviceconnect.util.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/technicians")
@RequiredArgsConstructor
public class TechnicianController {

    private final TechnicianRepository technicianRepository;
    private final UserMapper userMapper;

    /**
     * GET /technicians/:id/location
     * Returns the current location of a technician.
     */
    @GetMapping("/{id}/location")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTechnicianLocation(
            @PathVariable Long id) {
        Technician tech = technicianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found: " + id));

        Map<String, Object> location = new HashMap<>();
        location.put("technicianId",   tech.getId());
        location.put("technicianName", tech.getName());
        location.put("lat",            tech.getLocationLat() != null ? tech.getLocationLat() : 0.0);
        location.put("lng",            tech.getLocationLng() != null ? tech.getLocationLng() : 0.0);
        location.put("address",        tech.getLocationAddress() != null ? tech.getLocationAddress() : "");
        location.put("availability",   tech.getAvailability().name());
        location.put("status",         tech.getStatus() != null ? tech.getStatus().name() : null);
        location.put("accountStatus",  tech.getAccountStatus() != null ? tech.getAccountStatus().name() : null);
        location.put("region",         tech.getRegion());
        location.put("district",       tech.getDistrict());
        location.put("serviceTypes",   tech.getServiceTypes());

        return ResponseEntity.ok(ApiResponse.success(location));
    }

    /**
     * GET /technicians/:id
     * Returns a technician's public profile.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getTechnicianProfile(@PathVariable Long id) {
        Technician tech = technicianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found: " + id));
        return ResponseEntity.ok(ApiResponse.success(userMapper.toResponse(tech)));
    }

    /**
     * GET /technicians/available
     * Debug endpoint: lists all available technicians with their details.
     */
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAvailableTechnicians(
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double maxDistanceKm) {

        List<Technician> available;

        if (lat != null && lng != null && maxDistanceKm != null) {
            available = technicianRepository.findAvailableByServiceTypeAndLocation(
                    serviceType != null ? serviceType : "", lat, lng, maxDistanceKm, LocalDateTime.now());
        } else if (serviceType != null) {
            available = technicianRepository.findAvailableByRegionAndDistrict("Dar es Salaam", "Kinondoni", LocalDateTime.now())
                    .stream()
                    .filter(t -> t.getServiceTypes() != null && t.getServiceTypes().contains(serviceType))
                    .collect(Collectors.toList());
        } else {
            available = technicianRepository.findByAvailability(Technician.Availability.available);
        }

        List<Map<String, Object>> details = available.stream()
                .map(t -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", t.getId());
                    map.put("name", t.getName());
                    map.put("rating", t.getRating());
                    map.put("totalJobs", t.getTotalJobs());
                    map.put("availability", t.getAvailability().name());
                    map.put("status", t.getStatus() != null ? t.getStatus().name() : null);
                    map.put("accountStatus", t.getAccountStatus() != null ? t.getAccountStatus().name() : null);
                    map.put("region", t.getRegion());
                    map.put("district", t.getDistrict());
                    map.put("locationLat", t.getLocationLat());
                    map.put("locationLng", t.getLocationLng());
                    map.put("serviceTypes", t.getServiceTypes());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(details));
    }
}
