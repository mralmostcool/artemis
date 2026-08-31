package com.mralmostcool.artemis.seafarer.internal.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "profile_indos_mapping", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileIndosMapping {

    @Id
    @Column(name = "profile_id")
    private UUID profileId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "indos_master_id", nullable = false, unique = true)
    private IndosMaster indosMaster;

    @Column(name = "linked_at", insertable = false, updatable = false)
    private OffsetDateTime linkedAt;
}
