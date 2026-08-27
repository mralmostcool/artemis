package com.mralmostcool.artemis.auth.internal.log;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthLogRepository extends JpaRepository<AuthLog, UUID> {

    @Query("SELECT al FROM AuthLog al WHERE :userId IS NULL OR al.user.id = :userId ORDER BY al.createdAt DESC")
    List<AuthLog> findAllFiltered(@Param("userId") UUID userId);

    @Query("SELECT al FROM AuthLog al WHERE :userId IS NULL OR al.user.id = :userId")
    Page<AuthLog> findAllFiltered(@Param("userId") UUID userId, Pageable pageable);
}
