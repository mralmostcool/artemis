package com.mralmostcool.artemis.seafarer.internal.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "seafarer_medical", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeafarerMedical {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "indos_master_id", nullable = false)
    private IndosMaster indosMaster;

    @Column(name = "doctor_name", nullable = false)
    private String doctorName;

    @Column(name = "doctor_registration_no", nullable = false)
    private String doctorRegistrationNo;

    @Column(name = "examination_date", nullable = false)
    private LocalDate examinationDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "is_fit", nullable = false)
    @Builder.Default
    private boolean isFit = true;

    private String remarks;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
