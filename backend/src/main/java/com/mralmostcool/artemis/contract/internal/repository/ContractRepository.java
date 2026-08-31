package com.mralmostcool.artemis.contract.internal.repository;

import com.mralmostcool.artemis.contract.internal.model.Contract;
import com.mralmostcool.artemis.contract.internal.model.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ContractRepository extends JpaRepository<Contract, UUID> {
    List<Contract> findByCompanyId(UUID companyId);
    List<Contract> findByIndosMasterId(UUID indosMasterId);
    List<Contract> findByStatus(ContractStatus status);
}
