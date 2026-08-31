package com.mralmostcool.artemis.auth.internal.repository;

import com.mralmostcool.artemis.auth.internal.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
}
