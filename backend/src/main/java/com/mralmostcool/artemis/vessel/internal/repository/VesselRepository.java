package com.mralmostcool.artemis.vessel.internal.repository;

import com.mralmostcool.artemis.vessel.internal.model.Vessel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VesselRepository extends JpaRepository<Vessel, UUID> {
    Optional<Vessel> findByImo(String imo);
    List<Vessel> findByCompanyId(UUID companyId);
}
