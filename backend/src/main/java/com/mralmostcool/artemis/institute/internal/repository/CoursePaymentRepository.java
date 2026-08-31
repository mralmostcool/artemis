package com.mralmostcool.artemis.institute.internal.repository;

import com.mralmostcool.artemis.institute.internal.model.CoursePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface CoursePaymentRepository extends JpaRepository<CoursePayment, UUID> {
    List<CoursePayment> findByEnrollmentId(UUID enrollmentId);
}
