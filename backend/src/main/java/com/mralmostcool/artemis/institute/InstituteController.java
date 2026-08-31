package com.mralmostcool.artemis.institute;

import com.mralmostcool.artemis.auth.internal.model.Profile;
import com.mralmostcool.artemis.auth.internal.model.Role;
import com.mralmostcool.artemis.auth.internal.security.UserPrincipal;
import com.mralmostcool.artemis.institute.internal.model.*;
import com.mralmostcool.artemis.seafarer.SeafarerService;
import com.mralmostcool.artemis.seafarer.internal.model.ProfileIndosMapping;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/institutes")
public class InstituteController {

    private final InstituteService instituteService;
    private final SeafarerService seafarerService;

    public InstituteController(InstituteService instituteService, SeafarerService seafarerService) {
        this.instituteService = instituteService;
        this.seafarerService = seafarerService;
    }

    @GetMapping
    public ResponseEntity<List<Institute>> getAllInstitutes() {
        return ResponseEntity.ok(instituteService.getAllInstitutes());
    }

    @PostMapping
    public ResponseEntity<Institute> createInstitute(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Institute inst) {
        verifyRole(principal, Role.DG_SHIPPING_ADMIN);
        return ResponseEntity.status(HttpStatus.CREATED).body(instituteService.createInstitute(inst));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Institute> getInstitute(@PathVariable UUID id) {
        return instituteService.getInstitute(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Institute> updateInstitute(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody Institute inst) {
        verifyAnyRole(principal, Role.DG_SHIPPING_ADMIN, Role.INSTITUTE_ADMIN);
        return ResponseEntity.ok(instituteService.updateInstitute(id, inst));
    }

    @GetMapping("/{id}/courses")
    public ResponseEntity<List<PreSeaCourse>> getCourses(@PathVariable UUID id) {
        return ResponseEntity.ok(instituteService.getCoursesByInstitute(id));
    }

    @PostMapping("/{id}/courses")
    public ResponseEntity<PreSeaCourse> createCourse(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody PreSeaCourse course) {
        verifyAnyRole(principal, Role.DG_SHIPPING_ADMIN, Role.INSTITUTE_ADMIN);
        return ResponseEntity.status(HttpStatus.CREATED).body(instituteService.createCourse(id, course));
    }

    @PutMapping("/courses/{courseId}/quota")
    public ResponseEntity<PreSeaCourse> approveCourseQuota(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID courseId,
            @RequestParam Integer permittedCapacity,
            @RequestParam(required = false) String status) {
        verifyRole(principal, Role.DG_SHIPPING_ADMIN);
        return ResponseEntity.ok(instituteService.approveCourseQuota(courseId, permittedCapacity, status));
    }

    @PostMapping("/courses/{courseId}/checkout")
    public ResponseEntity<Enrollment> checkoutCourse(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID courseId) {
        Profile profile = principal.getProfile();
        if (profile == null || profile.getRole() != Role.CANDIDATE) {
            throw new AccessDeniedException("Only candidates can register and checkout pre-sea training courses");
        }
        ProfileIndosMapping mapping = seafarerService.getLink(profile.getId())
                .orElseThrow(() -> new IllegalArgumentException("Candidate profile must link to an INDoS number before enrolling"));
        return ResponseEntity.ok(instituteService.checkout(courseId, mapping.getIndosMaster().getId()));
    }

    @PostMapping("/payments/{paymentId}/confirm")
    public ResponseEntity<CoursePayment> confirmPayment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID paymentId,
            @RequestParam String reference) {
        // Allow public/system webhooks, or simple verification
        return ResponseEntity.ok(instituteService.confirmPayment(paymentId, reference));
    }

    @GetMapping("/candidates/{indosId}/enrollments")
    public ResponseEntity<List<Enrollment>> getCandidateEnrollments(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID indosId) {
        Profile profile = principal.getProfile();
        if (profile == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Candidate can only fetch their own timelines
        if (profile.getRole() == Role.CANDIDATE) {
            ProfileIndosMapping mapping = seafarerService.getLink(profile.getId())
                    .orElseThrow(() -> new AccessDeniedException("No INDoS record linked to profile"));
            if (!mapping.getIndosMaster().getId().equals(indosId)) {
                throw new AccessDeniedException("Forbidden: Candidates can only view their own enrollments timeline");
            }
        }
        
        return ResponseEntity.ok(instituteService.getEnrollmentsForCandidate(indosId));
    }

    @GetMapping("/enrollments/{enrollmentId}")
    public ResponseEntity<Enrollment> getEnrollmentDetails(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID enrollmentId) {
        return instituteService.getEnrollment(enrollmentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/enrollments/{enrollmentId}")
    public ResponseEntity<Enrollment> updateEnrollmentProgress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID enrollmentId,
            @RequestParam(required = false) EnrollmentStatus status,
            @RequestParam(required = false) Double attendance,
            @RequestParam(required = false) String grade) {
        verifyAnyRole(principal, Role.DG_SHIPPING_ADMIN, Role.INSTITUTE_ADMIN, Role.INSTITUTE_USER);
        return ResponseEntity.ok(instituteService.updateEnrollmentProgress(enrollmentId, status, attendance, grade));
    }

    private void verifyRole(UserPrincipal principal, Role requiredRole) {
        Profile profile = principal.getProfile();
        if (profile == null || profile.getRole() != requiredRole) {
            throw new AccessDeniedException("Forbidden: User lacks role " + requiredRole);
        }
    }

    private void verifyAnyRole(UserPrincipal principal, Role... requiredRoles) {
        Profile profile = principal.getProfile();
        if (profile != null) {
            for (Role role : requiredRoles) {
                if (profile.getRole() == role) {
                    return;
                }
            }
        }
        throw new AccessDeniedException("Forbidden: User lacks appropriate access roles");
    }
}
