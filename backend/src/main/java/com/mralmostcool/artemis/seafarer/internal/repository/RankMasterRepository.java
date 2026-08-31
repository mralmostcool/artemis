package com.mralmostcool.artemis.seafarer.internal.repository;

import com.mralmostcool.artemis.seafarer.internal.model.RankMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface RankMasterRepository extends JpaRepository<RankMaster, UUID> {
}
