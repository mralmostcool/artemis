package com.mralmostcool.artemis.seafarer;

import com.mralmostcool.artemis.auth.internal.model.Profile;
import com.mralmostcool.artemis.auth.internal.model.Role;
import com.mralmostcool.artemis.auth.internal.security.UserPrincipal;
import com.mralmostcool.artemis.seafarer.internal.model.IndosMaster;
import com.mralmostcool.artemis.seafarer.internal.model.ProfileIndosMapping;
import com.mralmostcool.artemis.seafarer.internal.model.RankMaster;
import com.mralmostcool.artemis.seafarer.internal.model.SeafarerMedical;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/seafarers")
public class SeafarerController {

    private final SeafarerService seafarerService;

    public SeafarerController(SeafarerService seafarerService) {
        this.seafarerService = seafarerService;
    }

    @GetMapping("/ranks")
    public ResponseEntity<List<RankMaster>> getAllRanks() {
        return ResponseEntity.ok(seafarerService.getAllRanks());
    }

    @PostMapping("/ranks")
    public ResponseEntity<RankMaster> createRank(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody RankMaster rank) {
        verifyRole(principal, Role.DG_SHIPPING_ADMIN);
        return ResponseEntity.status(HttpStatus.CREATED).body(seafarerService.createRank(rank));
    }

    @PutMapping("/ranks/{rankId}")
    public ResponseEntity<RankMaster> updateRank(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID rankId,
            @RequestBody RankMaster rank) {
        verifyRole(principal, Role.DG_SHIPPING_ADMIN);
        return ResponseEntity.ok(seafarerService.updateRank(rankId, rank));
    }

    @DeleteMapping("/ranks/{rankId}")
    public ResponseEntity<Void> deleteRank(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID rankId) {
        verifyRole(principal, Role.DG_SHIPPING_ADMIN);
        seafarerService.deleteRank(rankId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/indos")
    public ResponseEntity<List<IndosMaster>> querySeafarers(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String indos,
            @RequestParam(required = false) String firstName) {
        verifyAnyRole(principal, Role.DG_SHIPPING_ADMIN, Role.COMPANY_ADMIN, Role.COMPANY_USER, Role.INSTITUTE_ADMIN, Role.INSTITUTE_USER);
        return ResponseEntity.ok(seafarerService.querySeafarers(indos, firstName));
    }

    @GetMapping("/indos/{indosId}")
    public ResponseEntity<IndosMaster> getIndosDetails(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID indosId) {
        Profile profile = principal.getProfile();
        if (profile == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Candidates can only fetch their own mapped INDoS details
        if (profile.getRole() == Role.CANDIDATE) {
            ProfileIndosMapping mapping = seafarerService.getLink(profile.getId())
                    .orElseThrow(() -> new AccessDeniedException("No INDoS mapping found for profile"));
            if (!mapping.getIndosMaster().getId().equals(indosId)) {
                throw new AccessDeniedException("Candidates can only view their own INDoS records");
            }
        }
        
        return seafarerService.getIndosRecord(indosId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/indos")
    public ResponseEntity<IndosMaster> createIndos(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody IndosMaster record) {
        verifyRole(principal, Role.DG_SHIPPING_ADMIN);
        return ResponseEntity.status(HttpStatus.CREATED).body(seafarerService.createIndosRecord(record));
    }

    @PutMapping("/indos/{indosId}")
    public ResponseEntity<IndosMaster> updateIndos(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID indosId,
            @RequestBody IndosMaster record) {
        verifyRole(principal, Role.DG_SHIPPING_ADMIN);
        return ResponseEntity.ok(seafarerService.updateIndosRecord(indosId, record));
    }

    @PutMapping("/indos/{indosId}/status")
    public ResponseEntity<IndosMaster> toggleIndosStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID indosId,
            @RequestParam boolean active) {
        verifyRole(principal, Role.DG_SHIPPING_ADMIN);
        return ResponseEntity.ok(seafarerService.toggleIndosStatus(indosId, active));
    }

    @GetMapping("/link")
    public ResponseEntity<ProfileIndosMapping> getActiveLink(@AuthenticationPrincipal UserPrincipal principal) {
        Profile profile = principal.getProfile();
        if (profile == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return seafarerService.getLink(profile.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/link")
    public ResponseEntity<ProfileIndosMapping> linkProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String indos) {
        Profile profile = principal.getProfile();
        if (profile == null || profile.getRole() != Role.CANDIDATE) {
            throw new AccessDeniedException("Only candidates can link their profile to INDoS");
        }
        return ResponseEntity.ok(seafarerService.linkProfile(profile.getId(), indos));
    }

    @DeleteMapping("/link/{profileId}")
    public ResponseEntity<Void> unlinkProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID profileId) {
        verifyRole(principal, Role.DG_SHIPPING_ADMIN);
        seafarerService.unlinkProfile(profileId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{indosId}/medicals")
    public ResponseEntity<List<SeafarerMedical>> getMedicalRecords(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID indosId) {
        Profile profile = principal.getProfile();
        if (profile == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Candidates can only view their own medical reports
        if (profile.getRole() == Role.CANDIDATE) {
            ProfileIndosMapping mapping = seafarerService.getLink(profile.getId())
                    .orElseThrow(() -> new AccessDeniedException("No INDoS mapping found for profile"));
            if (!mapping.getIndosMaster().getId().equals(indosId)) {
                throw new AccessDeniedException("Candidates can only view their own medical records");
            }
        }

        return ResponseEntity.ok(seafarerService.getMedicalRecords(indosId));
    }

    @PostMapping("/{indosId}/medicals")
    public ResponseEntity<SeafarerMedical> createMedicalRecord(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID indosId,
            @RequestBody SeafarerMedical medical) {
        verifyAnyRole(principal, Role.DG_SHIPPING_ADMIN, Role.COMPANY_ADMIN);
        return ResponseEntity.status(HttpStatus.CREATED).body(seafarerService.createMedicalRecord(indosId, medical));
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
