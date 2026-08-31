package com.mralmostcool.artemis.vessel.internal.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "vessel", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vessel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String imo;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String flag;

    @Column(name = "vessel_type", nullable = false)
    @Builder.Default
    private String vesselType = "Bulk Carrier";

    @Column(name = "call_sign", unique = true)
    private String callSign;

    private Double grt;
    private Double nrt;

    @Column(name = "engine_power_kw")
    private Integer enginePowerKw;

    @Column(name = "year_built")
    private Integer yearBuilt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
