package com.mralmostcool.artemis.contract;

import com.mralmostcool.artemis.auth.internal.model.Role;
import com.mralmostcool.artemis.auth.internal.security.UserPrincipal;
import com.mralmostcool.artemis.contract.internal.model.Contract;
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
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @GetMapping("/contracts")
    public ResponseEntity<List<Contract>> getAllContracts(
            @AuthenticationPrincipal UserPrincipal principal) {
        verifyRole(principal, Role.DG_SHIPPING_ADMIN);
        return ResponseEntity.ok(contractService.getAllContracts());
    }

    @GetMapping("/contracts/{id}")
    public ResponseEntity<Contract> getContract(@PathVariable UUID id) {
        return contractService.getContract(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/contracts")
    public ResponseEntity<Contract> draftContract(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Contract contract) {
        verifyAnyRole(principal, Role.DG_SHIPPING_ADMIN, Role.COMPANY_ADMIN);
        return ResponseEntity.status(HttpStatus.CREATED).body(contractService.draftContract(contract));
    }

    @PutMapping("/contracts/{id}/extension")
    public ResponseEntity<Contract> extendContract(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam String extendedSignOffDate) {
        verifyAnyRole(principal, Role.DG_SHIPPING_ADMIN, Role.INSTITUTE_ADMIN);
        return ResponseEntity.ok(contractService.extendContract(id, OffsetDateTime.parse(extendedSignOffDate)));
    }

    @PostMapping("/contracts/{id}/sign-on")
    public ResponseEntity<Contract> signOn(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam String actualSignOnDate,
            @RequestParam String port,
            @RequestParam String country) {
        verifyAnyRole(principal, Role.DG_SHIPPING_ADMIN, Role.COMPANY_ADMIN, Role.COMPANY_USER);
        return ResponseEntity.ok(contractService.signOn(id, OffsetDateTime.parse(actualSignOnDate), port, country));
    }

    @PostMapping("/contracts/{id}/sign-off")
    public ResponseEntity<Contract> signOff(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam String actualSignOffDate,
            @RequestParam String port,
            @RequestParam String country,
            @RequestParam(required = false) String remarks) {
        verifyAnyRole(principal, Role.DG_SHIPPING_ADMIN, Role.COMPANY_ADMIN, Role.COMPANY_USER);
        return ResponseEntity.ok(contractService.signOff(id, OffsetDateTime.parse(actualSignOffDate), port, country, remarks));
    }

    @GetMapping("/companies/{companyId}/contracts")
    public ResponseEntity<List<Contract>> getCompanyContracts(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID companyId) {
        verifyAnyRole(principal, Role.DG_SHIPPING_ADMIN, Role.COMPANY_ADMIN);
        return ResponseEntity.ok(contractService.getContractsByCompany(companyId));
    }

    @GetMapping("/seafarers/{indosId}/contracts")
    public ResponseEntity<List<Contract>> getSeafarerContracts(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID indosId) {
        // Candidates can only see their own contract list
        return ResponseEntity.ok(contractService.getContractsBySeafarer(indosId));
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
