package com.mralmostcool.artemis.contract;

import com.mralmostcool.artemis.contract.internal.model.Contract;
import com.mralmostcool.artemis.contract.internal.model.ContractStatus;
import com.mralmostcool.artemis.institute.InstituteService;
import com.mralmostcool.artemis.institute.internal.model.*;
import com.mralmostcool.artemis.seafarer.SeafarerService;
import com.mralmostcool.artemis.seafarer.internal.model.IndosMaster;
import com.mralmostcool.artemis.seafarer.internal.model.RankMaster;
import com.mralmostcool.artemis.seafarer.internal.model.SeafarerMedical;
import com.mralmostcool.artemis.vessel.VesselService;
import com.mralmostcool.artemis.vessel.internal.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ContractIntegrationTests {

    @Autowired
    private ContractService contractService;

    @Autowired
    private SeafarerService seafarerService;

    @Autowired
    private VesselService vesselService;

    @Autowired
    private InstituteService instituteService;

    @Test
    @Transactional
    void testContractLifecycleFlow() {
        // Seed Institute and Course
        Institute inst = Institute.builder().name("Mumbai Academy").mtiCode("MTI-99").build();
        inst = instituteService.createInstitute(inst);

        PreSeaCourse course = PreSeaCourse.builder()
                .name("GP Rating")
                .courseCode("GPR-88")
                .durationDays(180)
                .cost(30000.0)
                .requestedCapacity(20)
                .startDate(LocalDate.of(2026, 10, 1))
                .build();
        course = instituteService.createCourse(inst.getId(), course);
        course = instituteService.approveCourseQuota(course.getId(), 20, "APPROVED");

        // Seed Rank and INDoS for candidate
        RankMaster rank = RankMaster.builder().name("Deck Cadet").level(1).build();
        rank = seafarerService.createRank(rank);

        IndosMaster indos = IndosMaster.builder()
                .indos("66ZZ888")
                .firstName("Charlie")
                .lastName("Brown")
                .rank(rank)
                .dateOfBirth(LocalDate.of(1999, 10, 12))
                .gender("MALE")
                .nationality("Indian")
                .build();
        indos = seafarerService.createIndosRecord(indos);

        // Enroll candidate and complete course
        Enrollment enrollment = instituteService.checkout(course.getId(), indos.getId());
        enrollment = instituteService.updateEnrollmentProgress(enrollment.getId(), EnrollmentStatus.COMPLETED, 95.0, "A");

        // Seed Shipping Company, Vessel, and Berth Allocation
        Company company = Company.builder().name("Star Shipping").rpslNo("RPSL-STAR-01").build();
        company = vesselService.createCompany(company);

        Vessel vessel = Vessel.builder().name("Star Voyager").imo("IMO9977").flag("Indian").build();
        vessel = vesselService.createVessel(company.getId(), vessel);

        Berth berth = Berth.builder().berthName("Berth 8").build();
        berth = vesselService.createBerth(berth);

        OffsetDateTime now = OffsetDateTime.now();
        BerthAllocation stay = vesselService.allocateBerthToVessel(berth.getId(), vessel.getId(), now, now.plusYears(1));
        BerthSeafarerAllocation alloc = vesselService.allocateBerthToSeafarer(berth.getId(), indos.getId(), stay.getId(), now, now.plusDays(150));

        // Draft Contract (Must be >= 100 days)
        Contract contract = Contract.builder()
                .indosMaster(indos)
                .company(company)
                .enrollment(enrollment)
                .berthSeafarerAllocation(alloc)
                .signOnDate(now)
                .signOnPort("Mumbai")
                .signOnCountry("India")
                .signOffDate(now.plusDays(120))
                .signOffPort("Singapore")
                .signOffCountry("Singapore")
                .nextOfKinName("Jane Brown")
                .nextOfKinRelation("Mother")
                .nextOfKinPhone("9988776655")
                .build();

        // Check validation for < 100 days stint fails
        contract.setSignOffDate(now.plusDays(50));
        assertThatThrownBy(() -> contractService.draftContract(contract))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Planned training stint must be a minimum of 100 days duration");

        // Check drafting valid contract
        contract.setSignOffDate(now.plusDays(120));
        Contract drafted = contractService.draftContract(contract);
        assertThat(drafted.getId()).isNotNull();
        assertThat(drafted.getStatus()).isEqualTo(ContractStatus.DRAFT);

        // Sign-on without medical fails
        assertThatThrownBy(() -> contractService.signOn(drafted.getId(), now, "Mumbai", "India"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No medical fitness records found");

        // Add fit medical
        SeafarerMedical medical = SeafarerMedical.builder()
                .doctorName("Dr. John")
                .doctorRegistrationNo("REG-12")
                .examinationDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusYears(1))
                .isFit(true)
                .build();
        seafarerService.createMedicalRecord(indos.getId(), medical);

        // Sign-on success
        Contract signedOn = contractService.signOn(drafted.getId(), now, "Mumbai", "India");
        assertThat(signedOn.getStatus()).isEqualTo(ContractStatus.ACTIVE);
        assertThat(signedOn.getActualSignOnPort()).isEqualTo("Mumbai");

        // Sign-off success
        Contract signedOff = contractService.signOff(signedOn.getId(), now.plusDays(110), "Singapore", "Singapore", "Cadet completed stint");
        assertThat(signedOff.getStatus()).isEqualTo(ContractStatus.COMPLETED);
        assertThat(signedOff.getActualSignOffPort()).isEqualTo("Singapore");

        // Verify concessions logged
        List<ConcessionLedger> concessions = vesselService.getConcessionsByCompany(company.getId());
        assertThat(concessions).isNotEmpty();
        assertThat(concessions.get(0).getCadetDaysLogged()).isEqualTo(110);
    }
}
