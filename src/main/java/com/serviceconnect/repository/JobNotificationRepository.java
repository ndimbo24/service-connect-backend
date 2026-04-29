package com.serviceconnect.repository;

import com.serviceconnect.entity.JobNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobNotificationRepository extends JpaRepository<JobNotification, Long> {
    List<JobNotification> findByTechnicianIdAndStatus(Long technicianId, JobNotification.NotificationStatus status);
    List<JobNotification> findByRequestId(Long requestId);
}
