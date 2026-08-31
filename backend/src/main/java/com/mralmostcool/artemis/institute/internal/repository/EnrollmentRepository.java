package com.mralmostcool.artemis.institute.internal.repository;

import com.mralmostcool.artemis.institute.internal.model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {
    List<Enrollment> findByIndosMasterId(UUID indosMasterId);
    List<Enrollment> findByPreSeaCourseId(UUID preSeaCourseId);
    Optional<Enrollment> findByPreSeaCourseIdAndIndosMasterId(UUID preSeaCourseId, UUID indosMasterId);
}
