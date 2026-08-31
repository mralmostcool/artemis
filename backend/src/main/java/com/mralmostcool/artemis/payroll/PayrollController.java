package com.mralmostcool.artemis.payroll;

import com.mralmostcool.artemis.auth.internal.model.Role;
import com.mralmostcool.artemis.auth.internal.security.UserPrincipal;
import com.mralmostcool.artemis.payroll.internal.model.PaySlip;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payroll")
public class PayrollController {

    private final PayrollService payrollService;

    public PayrollController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    @PostMapping("/runs")
    public ResponseEntity<List<PaySlip>> runPayroll(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "INR") String targetCurrency) {
        verifyRole(principal, Role.DG_SHIPPING_ADMIN);
        return ResponseEntity.ok(payrollService.generateMonthlyPaySlips(
                LocalDate.parse(startDate),
                LocalDate.parse(endDate),
                targetCurrency
        ));
    }

    @GetMapping("/slips/{id}")
    public ResponseEntity<PaySlip> getPaySlip(@PathVariable UUID id) {
        return payrollService.getPaySlip(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/slips/{id}/pay")
    public ResponseEntity<PaySlip> paySlip(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam String transactionReference) {
        verifyAnyRole(principal, Role.DG_SHIPPING_ADMIN, Role.COMPANY_ADMIN);
        return ResponseEntity.ok(payrollService.markPaid(id, transactionReference));
    }

    @GetMapping("/companies/{companyId}/slips")
    public ResponseEntity<List<PaySlip>> getCompanySlips(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID companyId) {
        verifyAnyRole(principal, Role.DG_SHIPPING_ADMIN, Role.COMPANY_ADMIN);
        return ResponseEntity.ok(payrollService.getPaySlipsByCompany(companyId));
    }

    @GetMapping("/seafarers/{indosId}/slips")
    public ResponseEntity<List<PaySlip>> getSeafarerSlips(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID indosId) {
        // Candidates can only retrieve their own pay slips
        return ResponseEntity.ok(payrollService.getPaySlipsBySeafarer(indosId));
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
