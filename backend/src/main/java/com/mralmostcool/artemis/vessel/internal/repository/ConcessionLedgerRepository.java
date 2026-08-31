package com.mralmostcool.artemis.vessel.internal.repository;

import com.mralmostcool.artemis.vessel.internal.model.ConcessionLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ConcessionLedgerRepository extends JpaRepository<ConcessionLedger, UUID> {
    List<ConcessionLedger> findByCompanyId(UUID companyId);
}
