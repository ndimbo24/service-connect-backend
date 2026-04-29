package com.serviceconnect.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "service_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long clientId;

    @Column(nullable = false)
    private String clientName;

    private Long technicianId;
    private String technicianName;

    @Column(nullable = false)
    private String serviceType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String voiceText;

    @Column(columnDefinition = "TEXT")
    private String imageData;

    private String aiCategory;
    private String aiSuggestedTechnicianType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RequestStatus status = RequestStatus.pending;

    // Location
    @Column(nullable = false)
    private Double locationLat;

    @Column(nullable = false)
    private Double locationLng;

    @Column(nullable = false)
    private String locationAddress;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;
    private LocalDateTime estimatedArrival;

    public enum RequestStatus {
        pending, searching, matched, in_progress, completed, cancelled, failed
    }

    // Manual getters for Lombok compatibility
    public String getAiSuggestedTechnicianType() {
        return aiSuggestedTechnicianType;
    }

    public String getServiceType() {
        return serviceType;
    }

    public Double getLocationLat() {
        return locationLat;
    }

    public Double getLocationLng() {
        return locationLng;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getClientId() {
        return clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public Long getTechnicianId() {
        return technicianId;
    }

    public String getTechnicianName() {
        return technicianName;
    }

    public String getDescription() {
        return description;
    }

    public String getVoiceText() {
        return voiceText;
    }

    public String getImageData() {
        return imageData;
    }

    public String getAiCategory() {
        return aiCategory;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public String getLocationAddress() {
        return locationAddress;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getEstimatedArrival() {
        return estimatedArrival;
    }

    // Manual setters for Lombok compatibility
    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public void setTechnicianId(Long technicianId) {
        this.technicianId = technicianId;
    }

    public void setEstimatedArrival(LocalDateTime estimatedArrival) {
        this.estimatedArrival = estimatedArrival;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }
}
