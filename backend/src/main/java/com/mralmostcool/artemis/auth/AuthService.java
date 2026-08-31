package com.mralmostcool.artemis.auth;

import com.mralmostcool.artemis.auth.internal.dto.ProfileRequest;
import com.mralmostcool.artemis.auth.internal.dto.ProfileResponse;
import com.mralmostcool.artemis.auth.internal.model.Organization;
import com.mralmostcool.artemis.auth.internal.model.Profile;
import com.mralmostcool.artemis.auth.internal.model.Role;
import com.mralmostcool.artemis.auth.internal.repository.OrganizationRepository;
import com.mralmostcool.artemis.auth.internal.repository.ProfileRepository;
import com.mralmostcool.artemis.auth.internal.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class AuthService {

    private final ProfileRepository profileRepository;
    private final OrganizationRepository organizationRepository;

    public AuthService(ProfileRepository profileRepository, OrganizationRepository organizationRepository) {
        this.profileRepository = profileRepository;
        this.organizationRepository = organizationRepository;
    }

    public ProfileResponse getProfile(UserPrincipal principal) {
        if (principal == null || principal.getProfile() == null) {
            return null;
        }
        return mapToResponse(principal.getProfile());
    }

    @Transactional
    public ProfileResponse registerProfile(UserPrincipal principal, ProfileRequest request) {
        UUID userId = UUID.fromString(principal.getJwt().getSubject());
        
        Profile existing = profileRepository.findById(userId).orElse(null);
        if (existing != null) {
            return mapToResponse(existing);
        }

        if (request.getDisplayName() == null || request.getDisplayName().isBlank()) {
            throw new IllegalArgumentException("Display Name is mandatory");
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
                .displayName(request.getDisplayName())
                .phoneNumber(request.getPhoneNumber())
                .organization(organization)
                .role(role)
                .build();

        profile = profileRepository.save(profile);
        return mapToResponse(profile);
    }

    @Transactional
    public ProfileResponse updateProfile(UserPrincipal principal, ProfileRequest request) {
        UUID userId = UUID.fromString(principal.getJwt().getSubject());
        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));

        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            profile.setDisplayName(request.getDisplayName());
        }
        
        if (request.getPhoneNumber() != null) {
            profile.setPhoneNumber(request.getPhoneNumber().isBlank() ? null : request.getPhoneNumber());
        }

        profile = profileRepository.save(profile);
        return mapToResponse(profile);
    }

    private ProfileResponse mapToResponse(Profile profile) {
        return ProfileResponse.builder()
                .id(profile.getId())
                .email(profile.getEmail())
                .displayName(profile.getDisplayName())
                .phoneNumber(profile.getPhoneNumber())
                .role(profile.getRole())
                .organizationId(profile.getOrganization() != null ? profile.getOrganization().getId() : null)
                .organizationName(profile.getOrganization() != null ? profile.getOrganization().getName() : null)
                .build();
    }
}
