package com.mralmostcool.artemis.certificate;

import com.mralmostcool.artemis.auth.internal.model.Profile;
import com.mralmostcool.artemis.auth.internal.model.Role;
import com.mralmostcool.artemis.auth.internal.repository.ProfileRepository;
import com.mralmostcool.artemis.certificate.internal.model.Certificate;
import com.mralmostcool.artemis.certificate.internal.model.CertificateStatus;
import com.mralmostcool.artemis.contract.ContractService;
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
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CertificateIntegrationTests {

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private ContractService contractService;

    @Autowired
    private SeafarerService seafarerService;

    @Autowired
    private VesselService vesselService;

    @Autowired
    private InstituteService instituteService;

    @Autowired
    private ProfileRepository profileRepository;

    @Test
    @Transactional
    void testCertificateWorkflowReviewAllotFlow() {
        // Seed Institute & Course
        Institute inst = Institute.builder().name("Delhi MTI").mtiCode("MTI-11").build();
        inst = instituteService.createInstitute(inst);

        PreSeaCourse course = PreSeaCourse.builder()
                .name("GP Course")
                .courseCode("GPR-11")
                .durationDays(180)
                .cost(10000.0)
                .requestedCapacity(10)
                .startDate(LocalDate.of(2026, 10, 1))
                .build();
        course = instituteService.createCourse(inst.getId(), course);
        course = instituteService.approveCourseQuota(course.getId(), 10, "APPROVED");

        // Seed Seafarer
        RankMaster rank = RankMaster.builder().name("Junior Deck Officer").level(1).build();
        rank = seafarerService.createRank(rank);

        IndosMaster indos = IndosMaster.builder()
                .indos("44ZZ102")
                .firstName("Frank")
                .lastName("Sinatra")
                .rank(rank)
                .dateOfBirth(LocalDate.of(2003, 1, 10))
                .gender("MALE")
                .nationality("Indian")
                .build();
        indos = seafarerService.createIndosRecord(indos);

        // Enroll & Complete
        Enrollment enrollment = instituteService.checkout(course.getId(), indos.getId());
        enrollment = instituteService.updateEnrollmentProgress(enrollment.getId(), EnrollmentStatus.COMPLETED, 98.0, "O");

        // Seed Company, Vessel, Stay Allocation
        Company company = Company.builder().name("Ocean Shipping").rpslNo("RPSL-OCEAN-01").build();
        company = vesselService.createCompany(company);

        Vessel vessel = Vessel.builder().name("Ocean Ranger").imo("IMO9911").flag("Indian").build();
        vessel = vesselService.createVessel(company.getId(), vessel);

        Berth berth = Berth.builder().berthName("Berth 11").build();
        berth = vesselService.createBerth(berth);

        OffsetDateTime now = OffsetDateTime.now();
        BerthAllocation stay = vesselService.allocateBerthToVessel(berth.getId(), vessel.getId(), now, now.plusYears(1));
        BerthSeafarerAllocation alloc = vesselService.allocateBerthToSeafarer(berth.getId(), indos.getId(), stay.getId(), now, now.plusDays(120));

        // Create Contract
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
                .nextOfKinName("Sarah")
                .nextOfKinRelation("Sister")
                .nextOfKinPhone("9988112233")
                .wageMonthlyUsd(1000.00)
                .build();
        Contract drafted = contractService.draftContract(contract);

        // Seed fit medical
        SeafarerMedical medical = SeafarerMedical.builder()
                .doctorName("Dr. Watson")
                .doctorRegistrationNo("REG-55")
                .examinationDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusYears(1))
                .isFit(true)
                .build();
        seafarerService.createMedicalRecord(indos.getId(), medical);

        // Sign-on to make active, then sign-off to complete
        Contract active = contractService.signOn(drafted.getId(), now, "Delhi", "India");
        Contract completed = contractService.signOff(active.getId(), now.plusDays(110), "Colombo", "Sri Lanka", "Good cadet");
        assertThat(completed.getStatus()).isEqualTo(ContractStatus.COMPLETED);

        // Enqueue Certificate (status: INITIATED)
        Certificate cert = certificateService.enqueueCertificate(completed.getId());
        assertThat(cert.getId()).isNotNull();
        assertThat(cert.getStatus()).isEqualTo(CertificateStatus.INITIATED);

        // Seed L1 & L2 Officers profiles
        Profile l1Officer = Profile.builder().id(UUID.randomUUID()).role(Role.DG_SHIPPING_L1).displayName("L1 Officer").build();
        Profile l2Officer = Profile.builder().id(UUID.randomUUID()).role(Role.DG_SHIPPING_L2).displayName("L2 Officer").build();
        l1Officer = profileRepository.save(l1Officer);
        l2Officer = profileRepository.save(l2Officer);

        // Level 1 review sign-off
        cert = certificateService.signOffL1(cert.getId(), l1Officer, "Cadet sea-days and exams verified");
        assertThat(cert.getStatus()).isEqualTo(CertificateStatus.REVIEWED_L1);
        assertThat(cert.getL1Officer().getDisplayName()).isEqualTo("L1 Officer");

        // Level 2 final sign-off
        cert = certificateService.approveL2(cert.getId(), l2Officer, "Final signature signed off");
        assertThat(cert.getStatus()).isEqualTo(CertificateStatus.APPROVED_L2);
        assertThat(cert.getL2Officer().getDisplayName()).isEqualTo("L2 Officer");

        // Shipping company allotment
        cert = certificateService.allotCertificate(cert.getId(), company.getId(), "CERT-OCEAN-11", LocalDate.now().plusYears(5));
        assertThat(cert.getStatus()).isEqualTo(CertificateStatus.ALLOTTED);
        assertThat(cert.getQrCodeHash()).isNotNull();
        assertThat(cert.getCertificateNumber()).isEqualTo("CERT-OCEAN-11");

        // Public check scanning verification
        Certificate verified = certificateService.verifyByQr(cert.getQrCodeHash()).orElseThrow();
        assertThat(verified.getCertificateNumber()).isEqualTo("CERT-OCEAN-11");
        assertThat(verified.getStatus()).isEqualTo(CertificateStatus.ALLOTTED);
    }
}
