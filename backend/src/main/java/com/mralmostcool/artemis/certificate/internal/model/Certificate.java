package com.mralmostcool.artemis.certificate.internal.model;

import com.mralmostcool.artemis.auth.internal.model.Profile;
import com.mralmostcool.artemis.contract.internal.model.Contract;
import com.mralmostcool.artemis.institute.internal.model.Enrollment;
import com.mralmostcool.artemis.seafarer.internal.model.IndosMaster;
import com.mralmostcool.artemis.vessel.internal.model.Company;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "certificates", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "indos_master_id", nullable = false)
    private IndosMaster indosMaster;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "contract_id", nullable = false, unique = true)
    private Contract contract;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CertificateStatus status = CertificateStatus.INITIATED;

    @Column(name = "certificate_type", nullable = false)
    @Builder.Default
    private String certificateType = "Certificate of Competency";

    @Column(name = "certificate_number", unique = true)
    private String certificateNumber;

    @Column(name = "qr_code_hash", unique = true)
    private String qrCodeHash;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "l1_officer_id")
    private Profile l1Officer;

    @Column(name = "l1_signed_at")
    private OffsetDateTime l1SignedAt;

    @Column(name = "l1_remarks")
    private String l1Remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "l2_officer_id")
    private Profile l2Officer;

    @Column(name = "l2_signed_at")
    private OffsetDateTime l2SignedAt;

    @Column(name = "l2_remarks")
    private String l2Remarks;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "allotted_by_company_id")
    private Company allottedByCompany;

    @Column(name = "allotted_at")
    private OffsetDateTime allottedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
