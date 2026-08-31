package com.mralmostcool.artemis.vessel.internal.repository;

import com.mralmostcool.artemis.vessel.internal.model.BerthSeafarerAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface BerthSeafarerAllocationRepository extends JpaRepository<BerthSeafarerAllocation, UUID> {
    List<BerthSeafarerAllocation> findByIndosMasterId(UUID indosMasterId);
    List<BerthSeafarerAllocation> findByBerthId(UUID berthId);
}
