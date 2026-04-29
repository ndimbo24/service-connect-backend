package com.serviceconnect.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class UpdateUserRequest {
    private String name;
    private String email;
    private String avatar;
    private String address;

    // Technician-specific
    private List<String> serviceTypes;
    private LocationData location;
    private String availability;

    @Data
    public static class LocationData {
        private Double lat;
        private Double lng;
        private String address;
    }
}
