package com.mralmostcool.artemis.vessel.internal.model;

import com.mralmostcool.artemis.seafarer.internal.model.IndosMaster;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "berth_seafarer_allocation", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BerthSeafarerAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "berth_id", nullable = false)
    private Berth berth;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "indos_master_id", nullable = false)
    private IndosMaster indosMaster;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "berth_allocation_id")
    private BerthAllocation berthAllocation;

    @Column(name = "start_date", nullable = false)
    private OffsetDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private OffsetDateTime endDate;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
