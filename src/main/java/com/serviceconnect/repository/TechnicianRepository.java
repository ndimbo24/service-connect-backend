package com.serviceconnect.repository;

import com.serviceconnect.entity.Technician;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TechnicianRepository extends JpaRepository<Technician, Long> {

    List<Technician> findByStatus(Technician.ApprovalStatus status);
    Page<Technician> findByStatus(Technician.ApprovalStatus status, Pageable pageable);

    List<Technician> findByAvailability(Technician.Availability availability);

    @Query("""
            SELECT DISTINCT t FROM Technician t
            LEFT JOIN FETCH t.serviceTypes
            WHERE t.availability = :availability
              AND t.status = 'approved'
              AND t.accountStatus = 'ACTIVE'
              AND t.subscriptionExpiryAt > :now
            """)
    List<Technician> findAvailableTechniciansWithServiceTypes(
            @Param("availability") Technician.Availability availability,
            @Param("now") LocalDateTime now);

    List<Technician> findByAccountStatus(Technician.AccountStatus accountStatus);

    // ← FIXED: changed LocalDate to LocalDateTime
    List<Technician> findByAccountStatusAndSubscriptionExpiryAtBefore(
            Technician.AccountStatus accountStatus,
            LocalDateTime expiryTime);

    @Query("""
            SELECT t FROM Technician t
            WHERE LOWER(t.region) = LOWER(:region)
              AND LOWER(t.district) = LOWER(:district)
              AND t.status = 'approved'
              AND t.accountStatus = 'ACTIVE'
              AND t.subscriptionExpiryAt > :now
              AND t.availability = 'available'
            """)
    List<Technician> findAvailableByRegionAndDistrict(
            @Param("region") String region,
            @Param("district") String district,
            @Param("now") LocalDateTime now);

    @Query("""
            SELECT t FROM Technician t
            WHERE LOWER(t.region) = LOWER(:region)
              AND t.status = 'approved'
              AND t.accountStatus = 'ACTIVE'
              AND t.subscriptionExpiryAt > :now
              AND t.availability = 'available'
            """)
    List<Technician> findAvailableByRegion(
            @Param("region") String region,
            @Param("now") LocalDateTime now);

    @Query("""
            SELECT t FROM Technician t
            WHERE :serviceType MEMBER OF t.serviceTypes
              AND t.status = 'approved'
              AND t.accountStatus = 'ACTIVE'
              AND t.subscriptionExpiryAt > :now
              AND t.availability = 'available'
              AND t.locationLat IS NOT NULL
              AND t.locationLng IS NOT NULL
              AND (6371 * acos(
                  cos(radians(:lat)) * cos(radians(t.locationLat)) * cos(radians(t.locationLng) - radians(:lng))
                  + sin(radians(:lat)) * sin(radians(t.locationLat))
              )) < :maxDistanceKm
            """)
    List<Technician> findAvailableByServiceTypeAndLocation(
            @Param("serviceType") String serviceType,
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("maxDistanceKm") Double maxDistanceKm,
            @Param("now") LocalDateTime now);

    @Query("""
            SELECT t FROM Technician t
            WHERE :serviceType MEMBER OF t.serviceTypes
              AND t.status = 'approved'
              AND t.accountStatus = 'ACTIVE'
              AND t.subscriptionExpiryAt > :now
              AND t.availability = 'available'
            """)
    List<Technician> findAvailableByServiceTypeOnly(
            @Param("serviceType") String serviceType,
            @Param("now") LocalDateTime now);

    @Query("""
            SELECT t FROM Technician t
            WHERE (LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(t.locationAddress) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND t.status = 'approved'
              AND t.accountStatus = 'ACTIVE'
            """)
    List<Technician> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT COUNT(t) FROM Technician t WHERE t.status = :status")
    long countByStatus(@Param("status") Technician.ApprovalStatus status);
}