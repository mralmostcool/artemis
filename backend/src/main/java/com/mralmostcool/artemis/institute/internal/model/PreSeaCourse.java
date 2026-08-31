package com.mralmostcool.artemis.institute.internal.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pre_sea_courses", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreSeaCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "course_code", nullable = false)
    private String courseCode;

    @Column(name = "duration_days", nullable = false)
    @Builder.Default
    private Integer durationDays = 180;

    @Column(nullable = false)
    @Builder.Default
    private Double cost = 0.00;

    @Column(name = "requested_capacity", nullable = false)
    @Builder.Default
    private Integer requestedCapacity = 40;

    @Column(name = "permitted_capacity")
    private Integer permittedCapacity;

    @Column(name = "quota_status", nullable = false)
    @Builder.Default
    private String quotaStatus = "PENDING"; // 'PENDING', 'APPROVED', 'REJECTED'

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "institute_id", nullable = false)
    private Institute institute;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
