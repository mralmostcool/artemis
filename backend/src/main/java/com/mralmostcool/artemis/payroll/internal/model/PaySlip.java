package com.mralmostcool.artemis.payroll.internal.model;

import com.mralmostcool.artemis.contract.internal.model.Contract;
import com.mralmostcool.artemis.seafarer.internal.model.IndosMaster;
import com.mralmostcool.artemis.vessel.internal.model.Company;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pay_slips", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaySlip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "indos_master_id", nullable = false)
    private IndosMaster indosMaster;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "pay_period_start", nullable = false)
    private LocalDate payPeriodStart;

    @Column(name = "pay_period_end", nullable = false)
    private LocalDate payPeriodEnd;

    @Column(name = "base_salary_usd", nullable = false)
    private Double baseSalaryUsd;

    @Column(name = "exchange_rate", nullable = false)
    @Builder.Default
    private Double exchangeRate = 1.000000;

    @Column(name = "target_currency", nullable = false)
    @Builder.Default
    private String targetCurrency = "INR";

    @Column(name = "payout_amount", nullable = false)
    private Double payoutAmount;

    @Column(name = "payment_status", nullable = false)
    @Builder.Default
    private String paymentStatus = "PENDING"; // 'PENDING', 'PAID', 'FAILED'

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "transaction_reference")
    private String transactionReference;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
