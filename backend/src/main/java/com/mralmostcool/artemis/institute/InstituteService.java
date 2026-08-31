package com.mralmostcool.artemis.institute;

import com.mralmostcool.artemis.institute.internal.model.*;
import com.mralmostcool.artemis.institute.internal.repository.*;
import com.mralmostcool.artemis.seafarer.SeafarerService;
import com.mralmostcool.artemis.seafarer.internal.model.IndosMaster;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InstituteService {

    private final InstituteRepository instituteRepository;
    private final PreSeaCourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CoursePaymentRepository paymentRepository;
    private final SeafarerService seafarerService;

    public InstituteService(InstituteRepository instituteRepository,
                            PreSeaCourseRepository courseRepository,
                            EnrollmentRepository enrollmentRepository,
                            CoursePaymentRepository paymentRepository,
                            SeafarerService seafarerService) {
        this.instituteRepository = instituteRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.paymentRepository = paymentRepository;
        this.seafarerService = seafarerService;
    }

    public List<Institute> getAllInstitutes() {
        return instituteRepository.findAll();
    }

    public Optional<Institute> getInstitute(UUID id) {
        return instituteRepository.findById(id);
    }

    @Transactional
    public Institute createInstitute(Institute inst) {
        if (inst.getMtiCode() == null || inst.getMtiCode().isBlank()) {
            throw new IllegalArgumentException("MTI Code is mandatory");
        }
        return instituteRepository.save(inst);
    }

    @Transactional
    public Institute updateInstitute(UUID id, Institute updated) {
        Institute inst = instituteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Institute not found"));
        if (updated.getName() != null) {
            inst.setName(updated.getName());
        }
        if (updated.getAddress() != null) {
            inst.setAddress(updated.getAddress());
        }
        if (updated.getCity() != null) {
            inst.setCity(updated.getCity());
        }
        if (updated.getWebsite() != null) {
            inst.setWebsite(updated.getWebsite());
        }
        return instituteRepository.save(inst);
    }

    public List<PreSeaCourse> getCoursesByInstitute(UUID instituteId) {
        return courseRepository.findByInstituteId(instituteId);
    }

    public Optional<PreSeaCourse> getCourse(UUID id) {
        return courseRepository.findById(id);
    }

    @Transactional
    public PreSeaCourse createCourse(UUID instituteId, PreSeaCourse course) {
        Institute inst = instituteRepository.findById(instituteId)
                .orElseThrow(() -> new IllegalArgumentException("Institute not found"));
        if (course.getStartDate() == null) {
            throw new IllegalArgumentException("Start date is mandatory");
        }
        if (course.getCourseCode() == null || course.getCourseCode().isBlank()) {
            throw new IllegalArgumentException("Course code is mandatory");
        }
        course.setInstitute(inst);
        course.setQuotaStatus("PENDING");
        course.setPermittedCapacity(null);
        return courseRepository.save(course);
    }

    @Transactional
    public PreSeaCourse approveCourseQuota(UUID courseId, Integer permittedCapacity, String status) {
        PreSeaCourse course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        if (permittedCapacity == null || permittedCapacity <= 0) {
            throw new IllegalArgumentException("Permitted capacity must be positive");
        }
        course.setPermittedCapacity(permittedCapacity);
        course.setQuotaStatus(status != null ? status : "APPROVED");
        return courseRepository.save(course);
    }

    @Transactional
    public Enrollment checkout(UUID courseId, UUID indosId) {
        PreSeaCourse course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        IndosMaster indos = seafarerService.getIndosRecord(indosId)
                .orElseThrow(() -> new IllegalArgumentException("INDoS record not found"));

        if (!course.getQuotaStatus().equals("APPROVED")) {
            throw new IllegalArgumentException("Course has not been permitted/approved by DG Shipping yet");
        }

        // Verify course limit capacity
        List<Enrollment> currentEnrollments = enrollmentRepository.findByPreSeaCourseId(courseId);
        long activeCount = currentEnrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ENROLLED || e.getStatus() == EnrollmentStatus.COMPLETED)
                .count();
        if (course.getPermittedCapacity() != null && activeCount >= course.getPermittedCapacity()) {
            throw new IllegalArgumentException("Course registration full! Permitted seat capacity reached.");
        }

        // Create enrollment
        Enrollment enrollment = Enrollment.builder()
                .preSeaCourse(course)
                .indosMaster(indos)
                .status(EnrollmentStatus.PENDING_PAYMENT)
                .build();
        enrollment = enrollmentRepository.save(enrollment);

        // Create initial payment
        CoursePayment payment = CoursePayment.builder()
                .enrollment(enrollment)
                .amount(course.getCost())
                .currency("INR")
                .paymentStatus("PENDING")
                .build();
        paymentRepository.save(payment);

        return enrollment;
    }

    @Transactional
    public CoursePayment confirmPayment(UUID paymentId, String gatewayReference) {
        CoursePayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment record not found"));
        
        if (payment.getPaymentStatus().equals("SUCCESS")) {
            return payment;
        }

        payment.setPaymentStatus("SUCCESS");
        payment.setGatewayReference(gatewayReference);
        payment.setCompletedAt(OffsetDateTime.now());
        payment = paymentRepository.save(payment);

        // Update enrollment status to ENROLLED
        Enrollment enrollment = payment.getEnrollment();
        enrollment.setStatus(EnrollmentStatus.ENROLLED);
        
        // Auto-assign roll number
        String roll = enrollment.getPreSeaCourse().getCourseCode() + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        enrollment.setRollNo(roll);
        enrollmentRepository.save(enrollment);

        return payment;
    }

    public List<Enrollment> getEnrollmentsForCandidate(UUID indosId) {
        return enrollmentRepository.findByIndosMasterId(indosId);
    }

    public List<Enrollment> getEnrollmentsForCourse(UUID courseId) {
        return enrollmentRepository.findByPreSeaCourseId(courseId);
    }

    public Optional<Enrollment> getEnrollment(UUID enrollmentId) {
        return enrollmentRepository.findById(enrollmentId);
    }

    @Transactional
    public Enrollment updateEnrollmentProgress(UUID enrollmentId, EnrollmentStatus status, Double attendance, String grade) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found"));
        
        if (status != null) {
            enrollment.setStatus(status);
            if (status == EnrollmentStatus.COMPLETED) {
                enrollment.setCertificateIssued(true);
            }
        }
        if (attendance != null) {
            enrollment.setAttendancePercentage(attendance);
        }
        if (grade != null) {
            enrollment.setGrade(grade);
        }
        
        return enrollmentRepository.save(enrollment);
    }
}
