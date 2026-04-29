package com.serviceconnect.repository;

import com.serviceconnect.entity.ServiceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {
    List<ServiceRequest> findByClientIdOrderByCreatedAtDesc(Long clientId);
    List<ServiceRequest> findByTechnicianIdOrderByCreatedAtDesc(Long technicianId);
    List<ServiceRequest> findByStatus(ServiceRequest.RequestStatus status);
    List<ServiceRequest> findAllByOrderByCreatedAtDesc();
    Page<ServiceRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Count service requests by status for dashboard stats.
     */
    @Query("SELECT COUNT(r) FROM ServiceRequest r WHERE r.status = :status")
    long countByStatus(@Param("status") ServiceRequest.RequestStatus status);

    /**
     * Count total service requests.
     */
    long count();
}
