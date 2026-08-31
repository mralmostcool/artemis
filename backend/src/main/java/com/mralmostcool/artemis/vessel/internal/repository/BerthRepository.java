package com.mralmostcool.artemis.vessel.internal.repository;

import com.mralmostcool.artemis.vessel.internal.model.Berth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BerthRepository extends JpaRepository<Berth, UUID> {
    Optional<Berth> findByBerthName(String berthName);
}
