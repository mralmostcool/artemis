package com.mralmostcool.artemis.institute.internal.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "institute", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Institute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "mti_code", nullable = false, unique = true)
    private String mtiCode;

    private String address;
    private String city;

    @Column(nullable = false)
    @Builder.Default
    private String country = "India";

    private String website;

    @Column(name = "approval_date")
    private LocalDate approvalDate;

    @Column(name = "approval_expiry_date")
    private LocalDate approvalExpiryDate;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
