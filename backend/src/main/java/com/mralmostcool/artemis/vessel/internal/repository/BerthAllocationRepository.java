package com.mralmostcool.artemis.vessel.internal.repository;

import com.mralmostcool.artemis.vessel.internal.model.BerthAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface BerthAllocationRepository extends JpaRepository<BerthAllocation, UUID> {
    List<BerthAllocation> findByVesselId(UUID vesselId);
    List<BerthAllocation> findByBerthId(UUID berthId);
}
