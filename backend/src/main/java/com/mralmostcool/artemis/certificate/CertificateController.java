package com.mralmostcool.artemis.certificate;

import com.mralmostcool.artemis.auth.internal.model.Profile;
import com.mralmostcool.artemis.auth.internal.model.Role;
import com.mralmostcool.artemis.auth.internal.security.UserPrincipal;
import com.mralmostcool.artemis.certificate.internal.model.Certificate;
import com.mralmostcool.artemis.certificate.internal.model.CertificateStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/certificates")
public class CertificateController {

    private final CertificateService certificateService;

    public CertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @GetMapping
    public ResponseEntity<List<Certificate>> getCertificates(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) CertificateStatus status) {
        verifyAnyRole(principal, Role.DG_SHIPPING_ADMIN, Role.DG_SHIPPING_L1, Role.DG_SHIPPING_L2);
        if (status != null) {
            return ResponseEntity.ok(certificateService.getCertificatesByStatus(status));
        }
        return ResponseEntity.ok(certificateService.getCertificatesByStatus(CertificateStatus.INITIATED));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Certificate> getCertificate(@PathVariable UUID id) {
        return certificateService.getCertificate(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/enqueue")
    public ResponseEntity<Certificate> enqueueCertificate(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam UUID contractId) {
        verifyAnyRole(principal, Role.DG_SHIPPING_ADMIN, Role.COMPANY_ADMIN);
        return ResponseEntity.status(HttpStatus.CREATED).body(certificateService.enqueueCertificate(contractId));
    }

    @PostMapping("/{id}/review/l1")
    public ResponseEntity<Certificate> reviewL1(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam String remarks) {
        verifyAnyRole(principal, Role.DG_SHIPPING_ADMIN, Role.DG_SHIPPING_L1);
        Profile officer = principal.getProfile();
        return ResponseEntity.ok(certificateService.signOffL1(id, officer, remarks));
    }

    @PostMapping("/{id}/review/l2")
    public ResponseEntity<Certificate> reviewL2(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam String remarks) {
        verifyAnyRole(principal, Role.DG_SHIPPING_ADMIN, Role.DG_SHIPPING_L2);
        Profile officer = principal.getProfile();
        return ResponseEntity.ok(certificateService.approveL2(id, officer, remarks));
    }

    @PostMapping("/{id}/allot")
    public ResponseEntity<Certificate> allotCertificate(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam UUID companyId,
            @RequestParam String certificateNo,
            @RequestParam String expiryDate) {
        verifyAnyRole(principal, Role.DG_SHIPPING_ADMIN, Role.COMPANY_ADMIN);
        return ResponseEntity.ok(certificateService.allotCertificate(
                id,
                companyId,
                certificateNo,
                LocalDate.parse(expiryDate)
        ));
    }

    @GetMapping("/verify/{qrHash}")
    public ResponseEntity<Certificate> verifyCertificate(@PathVariable String qrHash) {
        // Publicly accessible verification endpoint
        return certificateService.verifyByQr(qrHash)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private void verifyAnyRole(UserPrincipal principal, Role... requiredRoles) {
        if (principal.getProfile() != null) {
            for (Role role : requiredRoles) {
                if (principal.getProfile().getRole() == role) {
                    return;
                }
            }
        }
        throw new AccessDeniedException("Forbidden: User lacks appropriate access roles");
    }
}
