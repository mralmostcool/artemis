package com.mralmostcool.artemis.auth.internal.repository;

import com.mralmostcool.artemis.auth.internal.model.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, UUID> {
}
