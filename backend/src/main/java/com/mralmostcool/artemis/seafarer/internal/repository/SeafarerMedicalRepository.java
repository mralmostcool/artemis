package com.mralmostcool.artemis.seafarer.internal.repository;

import com.mralmostcool.artemis.seafarer.internal.model.SeafarerMedical;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SeafarerMedicalRepository extends JpaRepository<SeafarerMedical, UUID> {
    List<SeafarerMedical> findByIndosMasterIdOrderByExaminationDateDesc(UUID indosMasterId);
    Optional<SeafarerMedical> findFirstByIndosMasterIdOrderByExaminationDateDesc(UUID indosMasterId);
}
