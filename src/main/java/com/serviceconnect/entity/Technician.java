package com.serviceconnect.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "technicians")
@DiscriminatorValue("technician")
@PrimaryKeyJoinColumn(name = "user_id")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Technician extends User {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "technician_service_types", joinColumns = @JoinColumn(name = "technician_id"))
    @Column(name = "service_type")
    private List<String> serviceTypes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApprovalStatus status = ApprovalStatus.pending;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability", nullable = false)
    private Availability availability = Availability.offline;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 30)
    private AccountStatus accountStatus = AccountStatus.PENDING_PAYMENT;

    @Column(name = "subscription_expiry_at")
    private LocalDate subscriptionExpiryAt;

    private Double rating = 0.0;

    private Integer totalJobs = 0;

    // Legacy location fields (kept for backward compatibility, do NOT use for matching)
    @Column(name = "location_lat")
    private Double locationLat;

    @Column(name = "location_lng")
    private Double locationLng;

    @Column(name = "location_address")
    private String locationAddress;

    // New structured location fields — REQUIRED for technician work area
    @Column(name = "region", nullable = false, length = 100)
    private String region;

    @Column(name = "district", nullable = false, length = 100)
    private String district;

    @Column(name = "street", nullable = false, length = 200)
    private String street;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "technician_documents", joinColumns = @JoinColumn(name = "technician_id"))
    @Column(name = "document_url")
    private List<String> documents;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    public enum ApprovalStatus {
        pending, approved, rejected
    }

    public enum Availability {
        offline, available, busy
    }

    public enum AccountStatus {
        PENDING_PAYMENT, ACTIVE, SUSPENDED, INACTIVE, BLOCKED
    }

    // Manual getters for Lombok compatibility
    public Double getLocationLat() {
        return locationLat;
    }

    public Double getLocationLng() {
        return locationLng;
    }

    public Long getId() {
        return super.getId();
    }

    public Integer getTotalJobs() {
        return totalJobs;
    }

    public List<String> getServiceTypes() {
        return serviceTypes;
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public Availability getAvailability() {
        return availability;
    }

    public Double getRating() {
        return rating;
    }

    public List<String> getDocuments() {
        return documents;
    }

    public LocalDate getSubscriptionExpiryAt() {
        return subscriptionExpiryAt;
    }

    public String getRegion() {
        return region;
    }

    public String getDistrict() {
        return district;
    }

    public String getStreet() {
        return street;
    }

    public String getLocationAddress() {
        return locationAddress;
    }

    // Explicit setters for Lombok/compiler reliability
    public void setAvailability(Availability availability) {
        this.availability = availability;
    }

    public void setTotalJobs(int totalJobs) {
        this.totalJobs = totalJobs;
    }

    public void setStatus(ApprovalStatus status) {
        this.status = status;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public void setSubscriptionExpiryAt(LocalDate subscriptionExpiryAt) {
        this.subscriptionExpiryAt = subscriptionExpiryAt;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
