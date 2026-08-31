package com.mralmostcool.artemis.vessel;

import com.mralmostcool.artemis.seafarer.SeafarerService;
import com.mralmostcool.artemis.seafarer.internal.model.IndosMaster;
import com.mralmostcool.artemis.vessel.internal.model.*;
import com.mralmostcool.artemis.vessel.internal.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VesselService {

    private final CompanyRepository companyRepository;
    private final VesselRepository vesselRepository;
    private final BerthRepository berthRepository;
    private final BerthAllocationRepository berthAllocationRepository;
    private final TrainingBerthRequestRepository trainingRequestRepository;
    private final BerthSeafarerAllocationRepository seafarerAllocationRepository;
    private final ConcessionLedgerRepository concessionRepository;
    private final SeafarerService seafarerService;

    public VesselService(CompanyRepository companyRepository,
                         VesselRepository vesselRepository,
                         BerthRepository berthRepository,
                         BerthAllocationRepository berthAllocationRepository,
                         TrainingBerthRequestRepository trainingRequestRepository,
                         BerthSeafarerAllocationRepository seafarerAllocationRepository,
                         ConcessionLedgerRepository concessionRepository,
                         SeafarerService seafarerService) {
        this.companyRepository = companyRepository;
        this.vesselRepository = vesselRepository;
        this.berthRepository = berthRepository;
        this.berthAllocationRepository = berthAllocationRepository;
        this.trainingRequestRepository = trainingRequestRepository;
        this.seafarerAllocationRepository = seafarerAllocationRepository;
        this.concessionRepository = concessionRepository;
        this.seafarerService = seafarerService;
    }

    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    public Optional<Company> getCompany(UUID id) {
        return companyRepository.findById(id);
    }

    @Transactional
    public Company createCompany(Company comp) {
        if (comp.getRpslNo() == null || comp.getRpslNo().isBlank()) {
            throw new IllegalArgumentException("RPSL Number is mandatory");
        }
        return companyRepository.save(comp);
    }

    @Transactional
    public Company updateCompany(UUID id, Company updated) {
        Company comp = companyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));
        if (updated.getName() != null) {
            comp.setName(updated.getName());
        }
        if (updated.getRpslNo() != null) {
            comp.setRpslNo(updated.getRpslNo());
        }
        if (updated.getRpslValidUntil() != null) {
            comp.setRpslValidUntil(updated.getRpslValidUntil());
        }
        return companyRepository.save(comp);
    }

    public List<Vessel> getVesselsByCompany(UUID companyId) {
        return vesselRepository.findByCompanyId(companyId);
    }

    public Optional<Vessel> getVessel(UUID id) {
        return vesselRepository.findById(id);
    }

    @Transactional
    public Vessel createVessel(UUID companyId, Vessel vessel) {
        Company comp = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));
        if (vessel.getImo() == null || vessel.getImo().isBlank()) {
            throw new IllegalArgumentException("IMO number is mandatory");
        }
        vessel.setCompany(comp);
        return vesselRepository.save(vessel);
    }

    public List<Berth> getAllBerths() {
        return berthRepository.findAll();
    }

    @Transactional
    public Berth createBerth(Berth berth) {
        return berthRepository.save(berth);
    }

    @Transactional
    public BerthAllocation allocateBerthToVessel(UUID berthId, UUID vesselId, OffsetDateTime start, OffsetDateTime end) {
        Berth berth = berthRepository.findById(berthId)
                .orElseThrow(() -> new IllegalArgumentException("Berth not found"));
        Vessel vessel = vesselRepository.findById(vesselId)
                .orElseThrow(() -> new IllegalArgumentException("Vessel not found"));

        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        // Validate 1-year stay increments constraint
        OffsetDateTime oneYearLater = start.plusYears(1);
        if (end.isBefore(oneYearLater)) {
            throw new IllegalArgumentException("Berth stays must be booked for a minimum duration of 1 year");
        }

        // Physical validation limits (Vessel grt/nrt or physical limitations are simulated, we verify loa/draft)
        if (berth.getMaxDraftMeters() < 5.0) { // simple example physical check
            throw new IllegalArgumentException("Berth max draft limits exceeded for vessel specifications");
        }

        BerthAllocation allocation = BerthAllocation.builder()
                .berth(berth)
                .vessel(vessel)
                .startDate(start)
                .endDate(end)
                .build();

        return berthAllocationRepository.save(allocation);
    }

    @Transactional
    public TrainingBerthRequest createTrainingBerthRequest(UUID vesselId, TrainingBerthRequest request) {
        Vessel vessel = vesselRepository.findById(vesselId)
                .orElseThrow(() -> new IllegalArgumentException("Vessel not found"));
        request.setVessel(vessel);
        request.setStatus("PENDING");
        return trainingRequestRepository.save(request);
    }

    @Transactional
    public TrainingBerthRequest approveTrainingBerthRequest(UUID reqId, Integer approvedSlots, Double concessionRate) {
        TrainingBerthRequest request = trainingRequestRepository.findById(reqId)
                .orElseThrow(() -> new IllegalArgumentException("Training berth request not found"));
        request.setApprovedSlots(approvedSlots);
        request.setConcessionRatePerDayUsd(concessionRate);
        request.setStatus("APPROVED");
        return trainingRequestRepository.save(request);
    }

    @Transactional
    public BerthSeafarerAllocation allocateBerthToSeafarer(UUID berthId, UUID indosId, UUID berthAllocationId, OffsetDateTime start, OffsetDateTime end) {
        Berth berth = berthRepository.findById(berthId)
                .orElseThrow(() -> new IllegalArgumentException("Berth not found"));
        IndosMaster indos = seafarerService.getIndosRecord(indosId)
                .orElseThrow(() -> new IllegalArgumentException("INDoS record not found"));
        BerthAllocation berthAlloc = berthAllocationRepository.findById(berthAllocationId)
                .orElseThrow(() -> new IllegalArgumentException("Berth allocation stay not found"));

        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        BerthSeafarerAllocation alloc = BerthSeafarerAllocation.builder()
                .berth(berth)
                .indosMaster(indos)
                .berthAllocation(berthAlloc)
                .startDate(start)
                .endDate(end)
                .build();

        return seafarerAllocationRepository.save(alloc);
    }

    public List<BerthSeafarerAllocation> getSeafarerAllocations() {
        return seafarerAllocationRepository.findAll();
    }

    public List<BerthSeafarerAllocation> getAllocationsByBerth(UUID berthId) {
        return seafarerAllocationRepository.findByBerthId(berthId);
    }

    public List<BerthSeafarerAllocation> getAllocationsBySeafarer(UUID indosId) {
        return seafarerAllocationRepository.findByIndosMasterId(indosId);
    }

    @Transactional
    public ConcessionLedger logConcession(UUID allocationId, int cadetDaysLogged, double concessionRate) {
        BerthSeafarerAllocation allocation = seafarerAllocationRepository.findById(allocationId)
                .orElseThrow(() -> new IllegalArgumentException("Berth seafarer allocation not found"));
        
        Company company = allocation.getBerthAllocation().getVessel().getCompany();
        Vessel vessel = allocation.getBerthAllocation().getVessel();

        double concessionValue = cadetDaysLogged * concessionRate;

        ConcessionLedger ledger = ConcessionLedger.builder()
                .company(company)
                .vessel(vessel)
                .berthSeafarerAllocation(allocation)
                .cadetDaysLogged(cadetDaysLogged)
                .concessionValueUsd(concessionValue)
                .build();

        return concessionRepository.save(ledger);
    }

    public List<ConcessionLedger> getConcessionsByCompany(UUID companyId) {
        return concessionRepository.findByCompanyId(companyId);
    }

    public String generateImoCrewListJson(UUID vesselId) {
        Vessel vessel = vesselRepository.findById(vesselId)
                .orElseThrow(() -> new IllegalArgumentException("Vessel not found"));
        
        List<BerthAllocation> stays = berthAllocationRepository.findByVesselId(vesselId);
        List<UUID> berthIds = stays.stream().map(s -> s.getBerth().getId()).collect(Collectors.toList());
        
        // Find all seafarers currently active or allocated to berths
        List<BerthSeafarerAllocation> activeAllocations = seafarerAllocationRepository.findAll().stream()
                .filter(a -> berthIds.contains(a.getBerth().getId()))
                .collect(Collectors.toList());

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"imo\": \"").append(vessel.getImo()).append("\",\n");
        json.append("  \"vesselName\": \"").append(vessel.getName()).append("\",\n");
        json.append("  \"flag\": \"").append(vessel.getFlag()).append("\",\n");
        json.append("  \"callSign\": \"").append(vessel.getCallSign()).append("\",\n");
        json.append("  \"crewMembers\": [\n");

        for (int i = 0; i < activeAllocations.size(); i++) {
            BerthSeafarerAllocation alloc = activeAllocations.get(i);
            IndosMaster seafarer = alloc.getIndosMaster();
            json.append("    {\n");
            json.append("      \"indos\": \"").append(seafarer.getIndos()).append("\",\n");
            json.append("      \"firstName\": \"").append(seafarer.getFirstName()).append("\",\n");
            json.append("      \"lastName\": \"").append(seafarer.getLastName()).append("\",\n");
            json.append("      \"rank\": \"").append(seafarer.getRank().getName()).append("\",\n");
            json.append("      \"passportNo\": \"").append(seafarer.getPassportNo()).append("\",\n");
            json.append("      \"cdcNo\": \"").append(seafarer.getCdcNo()).append("\",\n");
            json.append("      \"gender\": \"").append(seafarer.getGender()).append("\",\n");
            json.append("      \"dateOfBirth\": \"").append(seafarer.getDateOfBirth()).append("\"\n");
            json.append("    }");
            if (i < activeAllocations.size() - 1) {
                json.append(",\n");
            }
        }
        json.append("\n  ]\n");
        json.append("}");
        return json.toString();
    }
}
