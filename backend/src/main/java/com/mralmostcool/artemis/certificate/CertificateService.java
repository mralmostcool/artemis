package com.mralmostcool.artemis.certificate;

import com.mralmostcool.artemis.auth.internal.model.Profile;
import com.mralmostcool.artemis.certificate.internal.model.Certificate;
import com.mralmostcool.artemis.certificate.internal.model.CertificateStatus;
import com.mralmostcool.artemis.certificate.internal.repository.CertificateRepository;
import com.mralmostcool.artemis.contract.ContractService;
import com.mralmostcool.artemis.contract.internal.model.Contract;
import com.mralmostcool.artemis.contract.internal.model.ContractStatus;
import com.mralmostcool.artemis.vessel.VesselService;
import com.mralmostcool.artemis.vessel.internal.model.Company;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final ContractService contractService;
    private final VesselService vesselService;

    public CertificateService(CertificateRepository certificateRepository,
                              ContractService contractService,
                              VesselService vesselService) {
        this.certificateRepository = certificateRepository;
        this.contractService = contractService;
        this.vesselService = vesselService;
    }

    public List<Certificate> getCertificatesBySeafarer(UUID indosId) {
        return certificateRepository.findByIndosMasterId(indosId);
    }

    public List<Certificate> getCertificatesByStatus(CertificateStatus status) {
        return certificateRepository.findByStatus(status);
    }

    public Optional<Certificate> getCertificate(UUID id) {
        return certificateRepository.findById(id);
    }

    @Transactional
    public Certificate enqueueCertificate(UUID contractId) {
        Contract contract = contractService.getContract(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found"));
        
        if (contract.getStatus() != ContractStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot issue certificate: training contract is not COMPLETED");
        }

        Certificate certificate = Certificate.builder()
                .contract(contract)
                .indosMaster(contract.getIndosMaster())
                .enrollment(contract.getEnrollment())
                .status(CertificateStatus.INITIATED)
                .build();

        return certificateRepository.save(certificate);
    }

    @Transactional
    public Certificate signOffL1(UUID certificateId, Profile l1Officer, String remarks) {
        Certificate cert = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found"));
        
        if (cert.getStatus() != CertificateStatus.INITIATED) {
            throw new IllegalArgumentException("L1 review can only be completed on INITIATED certificates");
        }

        cert.setL1Officer(l1Officer);
        cert.setL1SignedAt(OffsetDateTime.now());
        cert.setL1Remarks(remarks);
        cert.setStatus(CertificateStatus.REVIEWED_L1);

        return certificateRepository.save(cert);
    }

    @Transactional
    public Certificate approveL2(UUID certificateId, Profile l2Officer, String remarks) {
        Certificate cert = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found"));
        
        if (cert.getStatus() != CertificateStatus.REVIEWED_L1) {
            throw new IllegalArgumentException("L2 approval requires Level-1 REVIEWED_L1 state first");
        }

        cert.setL2Officer(l2Officer);
        cert.setL2SignedAt(OffsetDateTime.now());
        cert.setL2Remarks(remarks);
        cert.setStatus(CertificateStatus.APPROVED_L2);

        return certificateRepository.save(cert);
    }

    @Transactional
    public Certificate allotCertificate(UUID certificateId, UUID companyId, String certificateNo, LocalDate expiry) {
        Certificate cert = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found"));
        
        Company company = vesselService.getCompany(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Shipping Company not found"));

        if (cert.getStatus() != CertificateStatus.APPROVED_L2) {
            throw new IllegalArgumentException("Certificate allotment requires APPROVED_L2 status first");
        }

        cert.setAllottedByCompany(company);
        cert.setAllottedAt(OffsetDateTime.now());
        cert.setCertificateNumber(certificateNo);
        cert.setIssueDate(LocalDate.now());
        cert.setExpiryDate(expiry);
        cert.setQrCodeHash(generateSha256Hash(certificateNo + "-" + cert.getId()));
        cert.setStatus(CertificateStatus.ALLOTTED);

        return certificateRepository.save(cert);
    }

    @Transactional
    public Certificate rejectCertificate(UUID certificateId, String reason) {
        Certificate cert = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found"));
        cert.setStatus(CertificateStatus.REJECTED);
        cert.setL1Remarks("REJECTED: " + reason);
        return certificateRepository.save(cert);
    }

    public Optional<Certificate> verifyByQr(String qrHash) {
        return certificateRepository.findByQrCodeHash(qrHash);
    }

    private String generateSha256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 16); // Shortened hash for simple QR code links
        } catch (Exception ex) {
            return UUID.randomUUID().toString().substring(0, 8);
        }
    }
}
