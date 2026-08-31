package com.mralmostcool.artemis.institute.internal.repository;

import com.mralmostcool.artemis.institute.internal.model.PreSeaCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface PreSeaCourseRepository extends JpaRepository<PreSeaCourse, UUID> {
    List<PreSeaCourse> findByInstituteId(UUID instituteId);
    List<PreSeaCourse> findByIsActiveTrue();
}
