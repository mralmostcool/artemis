package com.mralmostcool.artemis.vessel.internal.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "concession_ledger", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConcessionLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vessel_id", nullable = false)
    private Vessel vessel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "berth_seafarer_allocation_id", nullable = false)
    private BerthSeafarerAllocation berthSeafarerAllocation;

    @Column(name = "cadet_days_logged", nullable = false)
    private Integer cadetDaysLogged;

    @Column(name = "concession_value_usd", nullable = false)
    private Double concessionValueUsd;

    @Column(name = "granted_at", insertable = false, updatable = false)
    private OffsetDateTime grantedAt;
}
