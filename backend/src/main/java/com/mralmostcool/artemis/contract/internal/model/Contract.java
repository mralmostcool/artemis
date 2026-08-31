package com.mralmostcool.artemis.contract.internal.model;

import com.mralmostcool.artemis.institute.internal.model.Enrollment;
import com.mralmostcool.artemis.seafarer.internal.model.IndosMaster;
import com.mralmostcool.artemis.vessel.internal.model.BerthSeafarerAllocation;
import com.mralmostcool.artemis.vessel.internal.model.Company;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "contract", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "indos_master_id", nullable = false)
    private IndosMaster indosMaster;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "berth_seafarer_allocation_id", nullable = false)
    private BerthSeafarerAllocation berthSeafarerAllocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ContractStatus status = ContractStatus.DRAFT;

    @Column(name = "wage_monthly_usd", nullable = false)
    @Builder.Default
    private Double wageMonthlyUsd = 1000.00;

    @Column(name = "agreement_type", nullable = false)
    @Builder.Default
    private String agreementType = "ITF Standard CBA";

    @Column(name = "rpsl_no", nullable = false)
    private String rpslNo;

    @Column(name = "next_of_kin_name", nullable = false)
    private String nextOfKinName;

    @Column(name = "next_of_kin_relation", nullable = false)
    private String nextOfKinRelation;

    @Column(name = "next_of_kin_phone", nullable = false)
    private String nextOfKinPhone;

    @Column(name = "sign_on_date", nullable = false)
    private OffsetDateTime signOnDate;

    @Column(name = "sign_on_port", nullable = false)
    private String signOnPort;

    @Column(name = "sign_on_country", nullable = false)
    private String signOnCountry;

    @Column(name = "sign_off_date", nullable = false)
    private OffsetDateTime signOffDate;

    @Column(name = "sign_off_port", nullable = false)
    private String signOffPort;

    @Column(name = "sign_off_country", nullable = false)
    private String signOffCountry;

    @Column(name = "actual_sign_on_date")
    private OffsetDateTime actualSignOnDate;

    @Column(name = "actual_sign_on_port")
    private String actualSignOnPort;

    @Column(name = "actual_sign_on_country")
    private String actualSignOnCountry;

    @Column(name = "actual_sign_off_date")
    private OffsetDateTime actualSignOffDate;

    @Column(name = "actual_sign_off_port")
    private String actualSignOffPort;

    @Column(name = "actual_sign_off_country")
    private String actualSignOffCountry;

    private String remarks;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
