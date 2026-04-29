package com.serviceconnect.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceRequestResponse {
    private Long id;
    private Long clientId;
    private String clientName;
    private Long technicianId;
    private String technicianName;
    private String serviceType;
    private String description;
    private String voiceText;
    private String imageData;
    private String aiCategory;
    private String aiSuggestedTechnicianType;
    private String status;
    private LocationData location;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime estimatedArrival;

    private TechnicianInfo technician;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationData {
        private Double lat;
        private Double lng;
        private String address;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TechnicianInfo {
        private Long id;
        private String name;
        private String phone;
        private String avatar;
        private Double rating;
        private Integer totalJobs;
        private List<String> serviceTypes;
        private LocationData location;
        private String availability;
    }
}
