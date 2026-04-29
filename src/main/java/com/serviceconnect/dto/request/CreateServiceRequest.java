package com.serviceconnect.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Objects;

@Data
public class CreateServiceRequest {
    @NotBlank(message = "Service type is required")
    private String serviceType;

    @NotBlank(message = "Description is required")
    private String description;

    private String voiceText;
    private String imageData;

    @NotNull(message = "Location is required")
    private LocationData location;

    // Explicit getters and setters for reliability
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getVoiceText() { return voiceText; }
    public void setVoiceText(String voiceText) { this.voiceText = voiceText; }
    public String getImageData() { return imageData; }
    public void setImageData(String imageData) { this.imageData = imageData; }
    public LocationData getLocation() { return location; }
    public void setLocation(LocationData location) { this.location = location; }

    @Data
    public static class LocationData {
        @NotNull
        private Double lat;

        @NotNull
        private Double lng;

        @NotBlank
        private String address;

        // Explicit getters and setters
        public Double getLat() { return lat; }
        public void setLat(Double lat) { this.lat = lat; }
        public Double getLng() { return lng; }
        public void setLng(Double lng) { this.lng = lng; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
    }
}
