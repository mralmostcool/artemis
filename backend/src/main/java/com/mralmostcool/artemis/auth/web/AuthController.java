package com.mralmostcool.artemis.auth.web;

import com.mralmostcool.artemis.auth.dto.ProfileRequest;
import com.mralmostcool.artemis.auth.dto.ProfileResponse;
import com.mralmostcool.artemis.auth.model.Organization;
import com.mralmostcool.artemis.auth.model.Profile;
import com.mralmostcool.artemis.auth.model.Role;
import com.mralmostcool.artemis.auth.repository.OrganizationRepository;
import com.mralmostcool.artemis.auth.repository.ProfileRepository;
import com.mralmostcool.artemis.auth.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final ProfileRepository profileRepository;
    private final OrganizationRepository organizationRepository;

    public AuthController(ProfileRepository profileRepository, OrganizationRepository organizationRepository) {
        this.profileRepository = profileRepository;
        this.organizationRepository = organizationRepository;
    }

    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null || principal.getProfile() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapToResponse(principal.getProfile()));
    }

    @PostMapping
    public ResponseEntity<ProfileResponse> registerProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody ProfileRequest request) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UUID userId = UUID.fromString(principal.getJwt().getSubject());
        
        Profile existing = profileRepository.findById(userId).orElse(null);
        if (existing != null) {
            return ResponseEntity.ok(mapToResponse(existing));
        }

        String email = principal.getJwt().getClaimAsString("email");
        Organization organization = null;
        Role role = request.getRole() != null ? request.getRole() : Role.EMPLOYEE;

        if (request.getOrganizationId() != null) {
            organization = organizationRepository.findById(request.getOrganizationId())
                    .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        } else if (request.getOrganizationName() != null && !request.getOrganizationName().isBlank()) {
            organization = Organization.builder()
                    .name(request.getOrganizationName())
                    .build();
            organization = organizationRepository.save(organization);
            role = Role.ADMIN;
        }

        Profile profile = Profile.builder()
                .id(userId)
                .email(email)
                .organization(organization)
                .role(role)
                .build();

        profile = profileRepository.save(profile);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(profile));
    }

    private ProfileResponse mapToResponse(Profile profile) {
        return ProfileResponse.builder()
                .id(profile.getId())
                .email(profile.getEmail())
                .role(profile.getRole())
                .organizationId(profile.getOrganization() != null ? profile.getOrganization().getId() : null)
                .organizationName(profile.getOrganization() != null ? profile.getOrganization().getName() : null)
                .build();
    }
}
