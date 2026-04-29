package com.serviceconnect.controller;

import com.serviceconnect.dto.response.ApiResponse;
import com.serviceconnect.entity.Technician;
import com.serviceconnect.repository.TechnicianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/debug")
@RequiredArgsConstructor
public class DebugController {

    private final TechnicianRepository technicianRepository;

    @GetMapping("/technicians-summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTechniciansSummary() {
        List<Technician> all = technicianRepository.findAll();

        Map<String, Object> summary = new HashMap<>();
        summary.put("total", all.size());

        List<Map<String, Object>> eligible = all.stream()
                .filter(t -> "approved".equalsIgnoreCase(t.getStatus() != null ? t.getStatus().name() : null)
                        && "ACTIVE".equalsIgnoreCase(t.getAccountStatus() != null ? t.getAccountStatus().name() : null)
                        && "available".equalsIgnoreCase(t.getAvailability() != null ? t.getAvailability().name() : null))
                .map(t -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", t.getId());
                    m.put("name", t.getName());
                    m.put("region", t.getRegion());
                    m.put("district", t.getDistrict());
                    m.put("locationLat", t.getLocationLat());
                    m.put("locationLng", t.getLocationLng());
                    m.put("serviceTypes", t.getServiceTypes());
                    m.put("status", t.getStatus() != null ? t.getStatus().name() : null);
                    m.put("accountStatus", t.getAccountStatus() != null ? t.getAccountStatus().name() : null);
                    m.put("availability", t.getAvailability() != null ? t.getAvailability().name() : null);
                    return m;
                })
                .collect(Collectors.toList());

        summary.put("eligibleForMatching", eligible);
        summary.put("countByStatus", all.stream()
                .collect(Collectors.groupingBy(t -> t.getStatus() != null ? t.getStatus().name() : "null", Collectors.counting())));
        summary.put("countByAccountStatus", all.stream()
                .collect(Collectors.groupingBy(t -> t.getAccountStatus() != null ? t.getAccountStatus().name() : "null", Collectors.counting())));
        summary.put("countByAvailability", all.stream()
                .collect(Collectors.groupingBy(t -> t.getAvailability() != null ? t.getAvailability().name() : "null", Collectors.counting())));

        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
