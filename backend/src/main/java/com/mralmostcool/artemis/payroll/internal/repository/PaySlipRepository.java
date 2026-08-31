package com.mralmostcool.artemis.payroll.internal.repository;

import com.mralmostcool.artemis.payroll.internal.model.PaySlip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaySlipRepository extends JpaRepository<PaySlip, UUID> {
    List<PaySlip> findByContractId(UUID contractId);
    List<PaySlip> findByIndosMasterId(UUID indosMasterId);
    List<PaySlip> findByCompanyId(UUID companyId);
}
