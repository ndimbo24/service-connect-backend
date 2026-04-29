package com.serviceconnect.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String role;
    private String avatar;
    private LocalDateTime createdAt;

    // Client-specific
    private String address;
    private LocationData location;

    // Technician-specific
    private List<String> serviceTypes;
    private String status;
    private String accountStatus;
    private String availability;
    private Double rating;
    private Integer totalJobs;
    private List<String> documents;
    private LocalDate subscriptionExpiryAt;

    // New structured location fields for technicians
    private String region;
    private String district;
    private String street;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationData {
        private Double lat;
        private Double lng;
        private String address;
    }
}
