package com.mralmostcool.artemis.seafarer.internal.repository;

import com.mralmostcool.artemis.seafarer.internal.model.ProfileIndosMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfileIndosMappingRepository extends JpaRepository<ProfileIndosMapping, UUID> {
    Optional<ProfileIndosMapping> findByProfileId(UUID profileId);
    Optional<ProfileIndosMapping> findByIndosMasterId(UUID indosMasterId);
}
