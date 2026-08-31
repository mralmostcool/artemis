package com.mralmostcool.artemis.payroll;

import com.mralmostcool.artemis.contract.ContractService;
import com.mralmostcool.artemis.contract.internal.model.Contract;
import com.mralmostcool.artemis.contract.internal.model.ContractStatus;
import com.mralmostcool.artemis.institute.InstituteService;
import com.mralmostcool.artemis.institute.internal.model.*;
import com.mralmostcool.artemis.payroll.internal.model.PaySlip;
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

@SpringBootTest
class PayrollIntegrationTests {

    @Autowired
    private PayrollService payrollService;

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
    void testPayrollGenerationDisbursementFlow() {
        // Seed Academic Course
        Institute inst = Institute.builder().name("Delhi Academy").mtiCode("MTI-10").build();
        inst = instituteService.createInstitute(inst);

        PreSeaCourse course = PreSeaCourse.builder()
                .name("GP Rating Course")
                .courseCode("GPR-10")
                .durationDays(180)
                .cost(20000.0)
                .requestedCapacity(10)
                .startDate(LocalDate.of(2026, 10, 1))
                .build();
        course = instituteService.createCourse(inst.getId(), course);
        course = instituteService.approveCourseQuota(course.getId(), 10, "APPROVED");

        // Seed Seafarer
        RankMaster rank = RankMaster.builder().name("Junior Deck Cadet").level(1).build();
        rank = seafarerService.createRank(rank);

        IndosMaster indos = IndosMaster.builder()
                .indos("55ZZ101")
                .firstName("David")
                .lastName("Miller")
                .rank(rank)
                .dateOfBirth(LocalDate.of(2002, 1, 10))
                .gender("MALE")
                .nationality("Indian")
                .build();
        indos = seafarerService.createIndosRecord(indos);

        // Enroll and Complete
        Enrollment enrollment = instituteService.checkout(course.getId(), indos.getId());
        enrollment = instituteService.updateEnrollmentProgress(enrollment.getId(), EnrollmentStatus.COMPLETED, 98.0, "O");

        // Seed Company, Vessel, Stay Allocation
        Company company = Company.builder().name("Globe Shipping").rpslNo("RPSL-GLOBE-01").build();
        company = vesselService.createCompany(company);

        Vessel vessel = Vessel.builder().name("Globe Pioneer").imo("IMO9910").flag("Indian").build();
        vessel = vesselService.createVessel(company.getId(), vessel);

        Berth berth = Berth.builder().berthName("Berth 10").build();
        berth = vesselService.createBerth(berth);

        OffsetDateTime now = OffsetDateTime.now();
        BerthAllocation stay = vesselService.allocateBerthToVessel(berth.getId(), vessel.getId(), now, now.plusYears(1));
        BerthSeafarerAllocation alloc = vesselService.allocateBerthToSeafarer(berth.getId(), indos.getId(), stay.getId(), now, now.plusDays(120));

        // Create active Contract
        Contract contract = Contract.builder()
                .indosMaster(indos)
                .company(company)
                .enrollment(enrollment)
                .berthSeafarerAllocation(alloc)
                .signOnDate(now)
                .signOnPort("Delhi")
                .signOnCountry("India")
                .signOffDate(now.plusDays(110))
                .signOffPort("Colombo")
                .signOffCountry("Sri Lanka")
                .nextOfKinName("Sarah Miller")
                .nextOfKinRelation("Sister")
                .nextOfKinPhone("9988112233")
                .wageMonthlyUsd(1200.00)
                .build();
        Contract drafted = contractService.draftContract(contract);

        // Add fit medical
        SeafarerMedical medical = SeafarerMedical.builder()
                .doctorName("Dr. Watson")
                .doctorRegistrationNo("REG-55")
                .examinationDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusYears(1))
                .isFit(true)
                .build();
        seafarerService.createMedicalRecord(indos.getId(), medical);

        // Sign-on to make active
        Contract active = contractService.signOn(drafted.getId(), now, "Delhi", "India");
        assertThat(active.getStatus()).isEqualTo(ContractStatus.ACTIVE);

        // Run payroll for active contracts (USD 1200 converts to INR 100,200 at mock rate 83.5)
        List<PaySlip> slips = payrollService.generateMonthlyPaySlips(LocalDate.now(), LocalDate.now().plusMonths(1), "INR");
        assertThat(slips).isNotEmpty();
        
        PaySlip targetSlip = slips.stream()
                .filter(s -> s.getContract().getId().equals(active.getId()))
                .findFirst().orElseThrow();
        
        assertThat(targetSlip.getBaseSalaryUsd()).isEqualTo(1200.00);
        assertThat(targetSlip.getExchangeRate()).isEqualTo(83.50);
        assertThat(targetSlip.getPayoutAmount()).isEqualTo(1200.00 * 83.50);
        assertThat(targetSlip.getPaymentStatus()).isEqualTo("PENDING");

        // Pay slip disbursement
        PaySlip paid = payrollService.markPaid(targetSlip.getId(), "TX-REF-9923812");
        assertThat(paid.getPaymentStatus()).isEqualTo("PAID");
        assertThat(paid.getPaidAt()).isNotNull();
        assertThat(paid.getTransactionReference()).isEqualTo("TX-REF-9923812");
    }
}
