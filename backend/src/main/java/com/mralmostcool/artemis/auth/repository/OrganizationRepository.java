package com.mralmostcool.artemis.auth.repository;

import com.mralmostcool.artemis.auth.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
}
