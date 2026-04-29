package com.serviceconnect.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrackingResponse {
    private Long requestId;
    private String requestStatus;
    private LocalDateTime estimatedArrival;

    private TechnicianLocation technicianLocation;
    private ClientLocation clientLocation;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TechnicianLocation {
        private Long technicianId;
        private String technicianName;
        private Double lat;
        private Double lng;
        private String address;
        private String availability;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientLocation {
        private Double lat;
        private Double lng;
        private String address;
    }
}
