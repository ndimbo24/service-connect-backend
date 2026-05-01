package com.serviceconnect.controller;

import com.serviceconnect.dto.response.ApiResponse;
import com.serviceconnect.dto.response.UserResponse;
import com.serviceconnect.entity.Technician;
import com.serviceconnect.exception.BadRequestException;
import com.serviceconnect.repository.TechnicianRepository;
import com.serviceconnect.util.LocationMapper;
import com.serviceconnect.util.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import java.time.LocalDate;
import java.util.Comparator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for searching available technicians by client GPS coordinates.
 *
 * <p>Strict rules:</p>
 * <ul>
 *   <li>Client MUST provide lat and lng</li>
 *   <li>System converts coordinates → region + district</li>
 *   <li>Matching is STRICTLY by region + district (no distance calculation)</li>
 *   <li>Only approved, ACTIVE, available technicians are returned</li>
 * </ul>
 */
@RestController
@RequestMapping("/technicians")
@RequiredArgsConstructor
public class TechnicianSearchController {

    private final TechnicianRepository technicianRepository;
    private final UserMapper userMapper;

    /**
     * Search available technicians near the client's GPS coordinates.
     *
     * @param lat latitude (required)
     * @param lng longitude (required)
     * @return list of matching technicians in the same region + district
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchByLocation(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {

        if (lat == null || lng == null) {
            throw new BadRequestException("Latitude and longitude are required.");
        }

        LocationMapper.RegionDistrict rd = LocationMapper.toRegionDistrict(lat, lng);

        if ("Unknown".equalsIgnoreCase(rd.region()) || "Unknown".equalsIgnoreCase(rd.district())) {
            throw new BadRequestException("Could not determine region and district from the provided coordinates.");
        }

        List<Technician> technicians = technicianRepository.findAvailableByRegionAndDistrict(
                rd.region(), rd.district(), LocalDate.now());

        List<Technician> sortedTechnicians = technicians.stream()
                .sorted(Comparator.comparingDouble(t -> getDistanceKm(lat, lng, t)))
                .toList();

        List<UserResponse> responses = sortedTechnicians.stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    private double getDistanceKm(double lat, double lng, Technician tech) {
        if (tech.getLocationLat() == null || tech.getLocationLng() == null) {
            return Double.MAX_VALUE;
        }
        double dLat = Math.toRadians(tech.getLocationLat() - lat);
        double dLon = Math.toRadians(tech.getLocationLng() - lng);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(tech.getLocationLat()))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}

