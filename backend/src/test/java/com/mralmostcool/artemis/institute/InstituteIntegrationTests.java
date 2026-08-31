package com.mralmostcool.artemis.institute;

import com.mralmostcool.artemis.institute.internal.model.*;
import com.mralmostcool.artemis.institute.internal.repository.*;
import com.mralmostcool.artemis.seafarer.SeafarerService;
import com.mralmostcool.artemis.seafarer.internal.model.IndosMaster;
import com.mralmostcool.artemis.seafarer.internal.model.RankMaster;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class InstituteIntegrationTests {

    @Autowired
    private InstituteService instituteService;

    @Autowired
    private SeafarerService seafarerService;

    @Autowired
    private CoursePaymentRepository paymentRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Test
    @Transactional
    void testInstituteCheckoutPaymentTimelineFlow() {
        Institute inst = Institute.builder()
                .name("Maritime Training Institute Mumbai")
                .mtiCode("MTI-9921")
                .isActive(true)
                .build();
        inst = instituteService.createInstitute(inst);
        assertThat(inst.getId()).isNotNull();

        PreSeaCourse course = PreSeaCourse.builder()
                .name("General Purpose Rating (GP)")
                .courseCode("GPR-99")
                .durationDays(180)
                .cost(50000.00)
                .requestedCapacity(40)
                .startDate(LocalDate.of(2026, 10, 1))
                .build();
        course = instituteService.createCourse(inst.getId(), course);
        assertThat(course.getId()).isNotNull();
        assertThat(course.getQuotaStatus()).isEqualTo("PENDING");

        // DG Shipping approves quota
        course = instituteService.approveCourseQuota(course.getId(), 40, "APPROVED");
        assertThat(course.getPermittedCapacity()).isEqualTo(40);
        assertThat(course.getQuotaStatus()).isEqualTo("APPROVED");

        // Seed Rank and INDoS for candidate
        RankMaster rank = RankMaster.builder().name("Trainee Cadet").level(1).build();
        rank = seafarerService.createRank(rank);

        IndosMaster indos = IndosMaster.builder()
                .indos("88ZZ999")
                .firstName("Alice")
                .lastName("Wonder")
                .rank(rank)
                .dateOfBirth(LocalDate.of(2000, 5, 20))
                .gender("FEMALE")
                .nationality("Indian")
                .build();
        indos = seafarerService.createIndosRecord(indos);

        // Candidate checks out course
        Enrollment enrollment = instituteService.checkout(course.getId(), indos.getId());
        assertThat(enrollment.getId()).isNotNull();
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.PENDING_PAYMENT);

        // Verify payment is generated
        List<CoursePayment> payments = paymentRepository.findByEnrollmentId(enrollment.getId());
        assertThat(payments).isNotEmpty();
        assertThat(payments.get(0).getPaymentStatus()).isEqualTo("PENDING");

        // Confirm Payment Webhook
        CoursePayment completedPayment = instituteService.confirmPayment(payments.get(0).getId(), "GATEWAY-REF-88712");
        assertThat(completedPayment.getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(completedPayment.getCompletedAt()).isNotNull();

        // Verify enrollment updated to ENROLLED and roll assigned
        Enrollment updatedEnrollment = enrollmentRepository.findById(enrollment.getId()).orElseThrow();
        assertThat(updatedEnrollment.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
        assertThat(updatedEnrollment.getRollNo()).startsWith("GPR-99-");

        // Verify status timelines retrieval
        List<Enrollment> timeline = instituteService.getEnrollmentsForCandidate(indos.getId());
        assertThat(timeline).isNotEmpty();
        assertThat(timeline.get(0).getPreSeaCourse().getName()).isEqualTo("General Purpose Rating (GP)");
    }
}
