package com.mralmostcool.artemis.vessel.internal.repository;

import com.mralmostcool.artemis.vessel.internal.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {
    Optional<Company> findByRpslNo(String rpslNo);
}
