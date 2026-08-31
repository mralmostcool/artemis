package com.mralmostcool.artemis.vessel.internal.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "training_berth_requests", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingBerthRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vessel_id", nullable = false)
    private Vessel vessel;

    @Column(name = "requested_slots", nullable = false)
    @Builder.Default
    private Integer requestedSlots = 1;

    @Column(name = "approved_slots")
    private Integer approvedSlots;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING"; // 'PENDING', 'APPROVED', 'REJECTED'

    @Column(name = "concession_rate_per_day_usd", nullable = false)
    @Builder.Default
    private Double concessionRatePerDayUsd = 5.00;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
