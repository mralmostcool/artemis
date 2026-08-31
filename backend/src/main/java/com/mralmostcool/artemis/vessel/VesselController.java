package com.mralmostcool.artemis.vessel;

import com.mralmostcool.artemis.auth.internal.model.Role;
import com.mralmostcool.artemis.auth.internal.security.UserPrincipal;
import com.mralmostcool.artemis.vessel.internal.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class VesselController {

    private final VesselService vesselService;

    public VesselController(VesselService vesselService) {
        this.vesselService = vesselService;
    }

    @GetMapping("/companies")
    public ResponseEntity<List<Company>> getAllCompanies() {
        return ResponseEntity.ok(vesselService.getAllCompanies());
    }

    @PostMapping("/companies")
    public ResponseEntity<Company> createCompany(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Company comp) {
        verifyRole(principal, Role.DG_SHIPPING_ADMIN);
        return ResponseEntity.status(HttpStatus.CREATED).body(vesselService.createCompany(comp));
    }

    @GetMapping("/companies/{id}")
    public ResponseEntity<Company> getCompany(@PathVariable UUID id) {
        return vesselService.getCompany(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/companies/{id}")
    public ResponseEntity<Company> updateCompany(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody Company comp) {
        verifyAnyRole(principal, Role.DG_SHIPPING_ADMIN, Role.COMPANY_ADMIN);
        return ResponseEntity.ok(vesselService.updateCompany(id, comp));
    }

    @GetMapping("/companies/{id}/vessels")
    public ResponseEntity<List<Vessel>> getVessels(@PathVariable UUID id) {
        return ResponseEntity.ok(vesselService.getVesselsByCompany(id));
    }

    @PostMapping("/companies/{id}/vessels")
    public ResponseEntity<Vessel> createVessel(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody Vessel vessel) {
        verifyAnyRole(principal, Role.DG_SHIPPING_ADMIN, Role.COMPANY_ADMIN);
        return ResponseEntity.status(HttpStatus.CREATED).body(vesselService.createVessel(id, vessel));
    }

    @GetMapping("/vessels/{id}")
    public ResponseEntity<Vessel> getVessel(@PathVariable UUID id) {
        return vesselService.getVessel(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/vessels/berths")
    public ResponseEntity<List<Berth>> getAllBerths() {
        return ResponseEntity.ok(vesselService.getAllBerths());
    }

    @PostMapping("/vessels/berths")
    public ResponseEntity<Berth> createBerth(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Berth berth) {
        verifyRole(principal, Role.DG_SHIPPING_ADMIN);
        return ResponseEntity.status(HttpStatus.CREATED).body(vesselService.createBerth(berth));
    }

    @PostMapping("/vessels/berth-allocations")
    public ResponseEntity<BerthAllocation> allocateBerthToVessel(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam UUID berthId,
            @RequestParam UUID vesselId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        verifyRole(principal, Role.DG_SHIPPING_ADMIN);
        return ResponseEntity.ok(vesselService.allocateBerthToVessel(
                berthId,
                vesselId,
                OffsetDateTime.parse(startDate),
                OffsetDateTime.parse(endDate)
        ));
    }

    @PostMapping("/vessels/{id}/training-berth-requests")
    public ResponseEntity<TrainingBerthRequest> createTrainingBerthRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody TrainingBerthRequest request) {
        verifyAnyRole(principal, Role.DG_SHIPPING_ADMIN, Role.COMPANY_ADMIN);
        return ResponseEntity.status(HttpStatus.CREATED).body(vesselService.createTrainingBerthRequest(id, request));
    }

    @PutMapping("/vessels/training-berth-requests/{reqId}")
    public ResponseEntity<TrainingBerthRequest> approveTrainingBerthRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID reqId,
            @RequestParam Integer approvedSlots,
            @RequestParam Double concessionRate) {
        verifyRole(principal, Role.DG_SHIPPING_ADMIN);
        return ResponseEntity.ok(vesselService.approveTrainingBerthRequest(reqId, approvedSlots, concessionRate));
    }

    @PostMapping("/vessels/seafarer-allocations")
    public ResponseEntity<BerthSeafarerAllocation> allocateBerthToSeafarer(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam UUID berthId,
            @RequestParam UUID indosId,
            @RequestParam UUID berthAllocationId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        verifyAnyRole(principal, Role.DG_SHIPPING_ADMIN, Role.COMPANY_ADMIN, Role.COMPANY_USER);
        return ResponseEntity.ok(vesselService.allocateBerthToSeafarer(
                berthId,
                indosId,
                berthAllocationId,
                OffsetDateTime.parse(startDate),
                OffsetDateTime.parse(endDate)
        ));
    }

    @GetMapping("/vessels/seafarer-allocations/timeline")
    public ResponseEntity<List<BerthSeafarerAllocation>> getTimelineCoordinates() {
        // Gantt chart endpoint showing active allocations
        return ResponseEntity.ok(vesselService.getSeafarerAllocations());
    }

    @GetMapping("/vessels/{id}/crew-list")
    public ResponseEntity<String> getImoCrewList(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        verifyAnyRole(principal, Role.DG_SHIPPING_ADMIN, Role.COMPANY_ADMIN, Role.COMPANY_USER);
        return ResponseEntity.ok(vesselService.generateImoCrewListJson(id));
    }

    @GetMapping("/companies/{id}/concessions")
    public ResponseEntity<List<ConcessionLedger>> getConcessionCredits(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        verifyAnyRole(principal, Role.DG_SHIPPING_ADMIN, Role.COMPANY_ADMIN);
        return ResponseEntity.ok(vesselService.getConcessionsByCompany(id));
    }

    private void verifyRole(UserPrincipal principal, Role requiredRole) {
        if (principal.getProfile() == null || principal.getProfile().getRole() != requiredRole) {
            throw new AccessDeniedException("Forbidden: User lacks role " + requiredRole);
        }
    }

    private void verifyAnyRole(UserPrincipal principal, Role... requiredRoles) {
        if (principal.getProfile() != null) {
            for (Role role : requiredRoles) {
                if (principal.getProfile().getRole() == role) {
                    return;
                }
            }
        }
        throw new AccessDeniedException("Forbidden: User lacks appropriate access roles");
    }
}
