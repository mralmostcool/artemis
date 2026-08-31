package com.mralmostcool.artemis.seafarer.internal.repository;

import com.mralmostcool.artemis.seafarer.internal.model.IndosMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IndosMasterRepository extends JpaRepository<IndosMaster, UUID> {
    Optional<IndosMaster> findByIndos(String indos);
    Optional<IndosMaster> findByPassportNo(String passportNo);
    Optional<IndosMaster> findByCdcNo(String cdcNo);
    List<IndosMaster> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(String firstName, String lastName);
}
