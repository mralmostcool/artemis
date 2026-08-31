package com.mralmostcool.artemis.institute.internal.repository;

import com.mralmostcool.artemis.institute.internal.model.Institute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstituteRepository extends JpaRepository<Institute, UUID> {
    Optional<Institute> findByMtiCode(String mtiCode);
}
