package com.mralmostcool.artemis.certificate.internal.repository;

import com.mralmostcool.artemis.certificate.internal.model.Certificate;
import com.mralmostcool.artemis.certificate.internal.model.CertificateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, UUID> {
    List<Certificate> findByIndosMasterId(UUID indosMasterId);
    List<Certificate> findByStatus(CertificateStatus status);
    Optional<Certificate> findByQrCodeHash(String qrCodeHash);
}
