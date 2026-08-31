package com.mralmostcool.artemis.vessel.internal.repository;

import com.mralmostcool.artemis.vessel.internal.model.TrainingBerthRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface TrainingBerthRequestRepository extends JpaRepository<TrainingBerthRequest, UUID> {
    List<TrainingBerthRequest> findByVesselId(UUID vesselId);
    List<TrainingBerthRequest> findByStatus(String status);
}
