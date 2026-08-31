package com.mralmostcool.artemis.vessel.internal.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "berth", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Berth {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "berth_name", nullable = false, unique = true)
    private String berthName;

    @Column(name = "max_draft_meters", nullable = false)
    @Builder.Default
    private Double maxDraftMeters = 10.00;

    @Column(name = "max_loa_meters", nullable = false)
    @Builder.Default
    private Double maxLoaMeters = 200.00;

    @Column(name = "coordinate_x")
    private Double coordinateX;

    @Column(name = "coordinate_y")
    private Double coordinateY;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
