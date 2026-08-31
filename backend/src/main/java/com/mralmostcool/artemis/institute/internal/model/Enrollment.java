package com.mralmostcool.artemis.institute.internal.model;

import com.mralmostcool.artemis.seafarer.internal.model.IndosMaster;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "enrollment", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pre_sea_course_id", nullable = false)
    private PreSeaCourse preSeaCourse;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "indos_master_id", nullable = false)
    private IndosMaster indosMaster;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EnrollmentStatus status = EnrollmentStatus.APPLIED;

    @Column(name = "roll_no")
    private String rollNo;

    @Column(name = "attendance_percentage")
    private Double attendancePercentage;

    private String grade;

    @Column(name = "certificate_issued", nullable = false)
    @Builder.Default
    private boolean certificateIssued = false;

    private String remarks;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
