package com.mralmostcool.artemis.seafarer.internal.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "indos_master", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndosMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 7)
    private String indos;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    @Builder.Default
    private String lastName = "";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rank_id", nullable = false)
    private RankMaster rank;

    @Column(name = "passport_no", unique = true)
    private String passportNo;

    @Column(name = "cdc_no", unique = true)
    private String cdcNo;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false)
    private String gender;

    @Column(nullable = false)
    @Builder.Default
    private String nationality = "Indian";

    @Column(name = "place_of_birth")
    private String placeOfBirth;

    @Column(name = "blood_group")
    private String bloodGroup;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
