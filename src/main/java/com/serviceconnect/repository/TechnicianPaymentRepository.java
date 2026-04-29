package com.serviceconnect.repository;

import com.serviceconnect.entity.TechnicianPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TechnicianPaymentRepository extends JpaRepository<TechnicianPayment, Long> {
    Optional<TechnicianPayment> findByReference(String reference);
    Optional<TechnicianPayment> findByOrderTrackingId(String orderTrackingId);

    List<TechnicianPayment> findByTechnicianIdAndStatusAndPaymentTypeOrderByDueDateAsc(
            Long technicianId,
            TechnicianPayment.PaymentStatus status,
            TechnicianPayment.PaymentType paymentType);
}
