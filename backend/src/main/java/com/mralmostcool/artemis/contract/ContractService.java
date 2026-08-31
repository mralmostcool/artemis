package com.mralmostcool.artemis.contract;

import com.mralmostcool.artemis.contract.internal.model.Contract;
import com.mralmostcool.artemis.contract.internal.model.ContractStatus;
import com.mralmostcool.artemis.contract.internal.repository.ContractRepository;
import com.mralmostcool.artemis.seafarer.SeafarerService;
import com.mralmostcool.artemis.seafarer.internal.model.SeafarerMedical;
import com.mralmostcool.artemis.vessel.VesselService;
import com.mralmostcool.artemis.vessel.internal.model.Company;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ContractService {

    private final ContractRepository contractRepository;
    private final SeafarerService seafarerService;
    private final VesselService vesselService;

    public ContractService(ContractRepository contractRepository,
            SeafarerService seafarerService,
            VesselService vesselService) {
        this.contractRepository = contractRepository;
        this.seafarerService = seafarerService;
        this.vesselService = vesselService;
    }

    public List<Contract> getAllContracts() {
        return contractRepository.findAll();
    }

    public Optional<Contract> getContract(UUID id) {
        return contractRepository.findById(id);
    }

    public List<Contract> getContractsByCompany(UUID companyId) {
        return contractRepository.findByCompanyId(companyId);
    }

    public List<Contract> getContractsBySeafarer(UUID indosId) {
        return contractRepository.findByIndosMasterId(indosId);
    }

    @Transactional
    public Contract draftContract(Contract contract) {
        if (contract.getSignOnDate() == null || contract.getSignOffDate() == null) {
            throw new IllegalArgumentException("Sign on and sign off dates are mandatory");
        }

        // Validate planned duration >= 100 days
        long days = Duration.between(contract.getSignOnDate(), contract.getSignOffDate()).toDays();
        if (days < 100) {
            throw new IllegalArgumentException("Planned training stint must be a minimum of 100 days duration");
        }

        // Prepopulate RPSL from Company
        Company company = vesselService.getCompany(contract.getCompany().getId())
                .orElseThrow(() -> new IllegalArgumentException("Shipping Company not found"));
        contract.setCompany(company);
        contract.setRpslNo(company.getRpslNo());
        contract.setStatus(ContractStatus.DRAFT);

        return contractRepository.save(contract);
    }

    @Transactional
    public Contract extendContract(UUID contractId, OffsetDateTime extendedSignOffDate) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found"));

        if (extendedSignOffDate.isBefore(contract.getSignOnDate())) {
            throw new IllegalArgumentException("Extended date must be after sign-on date");
        }

        contract.setSignOffDate(extendedSignOffDate);
        return contractRepository.save(contract);
    }

    @Transactional
    public Contract signOn(UUID contractId, OffsetDateTime actualSignOn, String port, String country) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found"));

        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft contracts can perform sign-on");
        }

        // Verify seafarer medical fitness compliance registry status
        List<SeafarerMedical> medicals = seafarerService.getMedicalRecords(contract.getIndosMaster().getId());
        if (medicals.isEmpty()) {
            throw new IllegalArgumentException("Cannot sign-on: No medical fitness records found for seafarer");
        }
        SeafarerMedical latest = medicals.get(0);
        if (!latest.isFit()) {
            throw new IllegalArgumentException("Cannot sign-on: Seafarer is medically UNFIT");
        }
        if (latest.getExpiryDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot sign-on: Seafarer medical fitness certificate has EXPIRED");
        }

        contract.setActualSignOnDate(actualSignOn);
        contract.setActualSignOnPort(port);
        contract.setActualSignOnCountry(country);
        contract.setStatus(ContractStatus.ACTIVE);

        return contractRepository.save(contract);
    }

    @Transactional
    public Contract signOff(UUID contractId, OffsetDateTime actualSignOff, String port, String country,
            String remarks) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found"));

        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active contracts can perform sign-off");
        }

        if (actualSignOff.isBefore(contract.getActualSignOnDate())) {
            throw new IllegalArgumentException("Sign-off date must be after sign-on date");
        }

        contract.setActualSignOffDate(actualSignOff);
        contract.setActualSignOffPort(port);
        contract.setActualSignOffCountry(country);
        contract.setStatus(ContractStatus.COMPLETED);
        contract.setRemarks(remarks);

        contract = contractRepository.save(contract);

        // Compute cadet days logged
        long cadetDays = Duration.between(contract.getActualSignOnDate(), actualSignOff).toDays();

        // Log cadet sea time concession values to vessel ledger
        vesselService.logConcession(
                contract.getBerthSeafarerAllocation().getId(),
                (int) cadetDays,
                8.00 // Default concession rate
        );

        return contract;
    }
}
