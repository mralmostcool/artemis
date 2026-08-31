package com.mralmostcool.artemis.payroll;

import com.mralmostcool.artemis.contract.ContractService;
import com.mralmostcool.artemis.contract.internal.model.Contract;
import com.mralmostcool.artemis.contract.internal.model.ContractStatus;
import com.mralmostcool.artemis.payroll.internal.model.PaySlip;
import com.mralmostcool.artemis.payroll.internal.repository.PaySlipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PayrollService {

    private final PaySlipRepository paySlipRepository;
    private final ContractService contractService;

    public PayrollService(PaySlipRepository paySlipRepository, ContractService contractService) {
        this.paySlipRepository = paySlipRepository;
        this.contractService = contractService;
    }

    public List<PaySlip> getPaySlipsBySeafarer(UUID indosId) {
        return paySlipRepository.findByIndosMasterId(indosId);
    }

    public List<PaySlip> getPaySlipsByCompany(UUID companyId) {
        return paySlipRepository.findByCompanyId(companyId);
    }

    public Optional<PaySlip> getPaySlip(UUID id) {
        return paySlipRepository.findById(id);
    }

    @Transactional
    public List<PaySlip> generateMonthlyPaySlips(LocalDate start, LocalDate end, String targetCurrency) {
        // Fetch all active contracts
        List<Contract> activeContracts = contractService.getAllContracts().stream()
                .filter(c -> c.getStatus() == ContractStatus.ACTIVE)
                .collect(Collectors.toList());

        List<PaySlip> generated = new ArrayList<>();
        double rate = getMockExchangeRate(targetCurrency);

        for (Contract contract : activeContracts) {
            double baseUsd = contract.getWageMonthlyUsd();
            double payout = baseUsd * rate;

            PaySlip slip = PaySlip.builder()
                    .contract(contract)
                    .indosMaster(contract.getIndosMaster())
                    .company(contract.getCompany())
                    .payPeriodStart(start)
                    .payPeriodEnd(end)
                    .baseSalaryUsd(baseUsd)
                    .exchangeRate(rate)
                    .targetCurrency(targetCurrency)
                    .payoutAmount(payout)
                    .paymentStatus("PENDING")
                    .build();

            generated.add(paySlipRepository.save(slip));
        }

        return generated;
    }

    @Transactional
    public PaySlip markPaid(UUID paySlipId, String reference) {
        PaySlip slip = paySlipRepository.findById(paySlipId)
                .orElseThrow(() -> new IllegalArgumentException("Pay slip not found"));
        slip.setPaymentStatus("PAID");
        slip.setTransactionReference(reference);
        slip.setPaidAt(OffsetDateTime.now());
        return paySlipRepository.save(slip);
    }

    private double getMockExchangeRate(String currency) {
        if (currency == null) {
            return 1.0;
        }
        return switch (currency.toUpperCase()) {
            case "INR" -> 83.50;
            case "EUR" -> 0.92;
            case "GBP" -> 0.79;
            case "JPY" -> 155.20;
            default -> 1.0;
        };
    }
}
