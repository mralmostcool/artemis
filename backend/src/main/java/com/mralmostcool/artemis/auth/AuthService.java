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
        
        Role role = request.getRole();
        if (role == null) {
            if (request.getOrganizationId() != null || (request.getOrganizationName() != null && !request.getOrganizationName().isBlank())) {
                role = Role.COMPANY_USER;
            } else {
                role = Role.CANDIDATE;
            }
        }

        if (request.getOrganizationId() != null) {
            organization = organizationRepository.findById(request.getOrganizationId())
                    .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        } else if (request.getOrganizationName() != null && !request.getOrganizationName().isBlank()) {
            organization = Organization.builder()
                    .name(request.getOrganizationName())
                    .build();
            organization = organizationRepository.save(organization);
            role = Role.COMPANY_ADMIN;
        }

        Profile profile = Profile.builder()
                .id(userId)
                .email(email)
                .firstName(request.getFirstName() != null ? request.getFirstName() : "")
                .lastName(request.getLastName() != null ? request.getLastName() : "")
                .displayName(request.getDisplayName())
                .phoneNumber(request.getPhoneNumber())
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .avatarUrl(request.getAvatarUrl())
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

        if (request.getFirstName() != null) {
            profile.setFirstName(request.getFirstName());
        }
        
        if (request.getLastName() != null) {
            profile.setLastName(request.getLastName());
        }

        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            profile.setDisplayName(request.getDisplayName());
        }
        
        if (request.getPhoneNumber() != null) {
            profile.setPhoneNumber(request.getPhoneNumber().isBlank() ? null : request.getPhoneNumber());
        }

        if (request.getGender() != null) {
            profile.setGender(request.getGender());
        }

        if (request.getDateOfBirth() != null) {
            profile.setDateOfBirth(request.getDateOfBirth());
        }

        if (request.getAvatarUrl() != null) {
            profile.setAvatarUrl(request.getAvatarUrl());
        }

        profile = profileRepository.save(profile);
        return mapToResponse(profile);
    }

    @Transactional
    public ProfileResponse updateUserStatus(UserPrincipal adminPrincipal, UUID targetUserId, boolean enabled) {
        Profile adminProfile = adminPrincipal.getProfile();
        if (adminProfile == null || (adminProfile.getRole() != Role.COMPANY_ADMIN && adminProfile.getRole() != Role.INSTITUTE_ADMIN && adminProfile.getRole() != Role.DG_SHIPPING_ADMIN)) {
            throw new org.springframework.security.access.AccessDeniedException("Only admins can disable/enable users");
        }

        UUID adminId = UUID.fromString(adminPrincipal.getJwt().getSubject());
        if (adminId.equals(targetUserId)) {
            throw new IllegalArgumentException("Admins cannot enable or disable themselves");
        }

        Profile targetProfile = profileRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user profile not found"));

        if (adminProfile.getRole() != Role.DG_SHIPPING_ADMIN) {
            if (adminProfile.getOrganization() == null || targetProfile.getOrganization() == null ||
                    !adminProfile.getOrganization().getId().equals(targetProfile.getOrganization().getId())) {
                throw new org.springframework.security.access.AccessDeniedException("Admin and target user must belong to the same organization");
            }
        }

        targetProfile.setEnabled(enabled);
        targetProfile = profileRepository.save(targetProfile);
        return mapToResponse(targetProfile);
    }

    private ProfileResponse mapToResponse(Profile profile) {
        return ProfileResponse.builder()
                .id(profile.getId())
                .email(profile.getEmail())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .displayName(profile.getDisplayName())
                .phoneNumber(profile.getPhoneNumber())
                .gender(profile.getGender())
                .dateOfBirth(profile.getDateOfBirth())
                .avatarUrl(profile.getAvatarUrl())
                .role(profile.getRole())
                .organizationId(profile.getOrganization() != null ? profile.getOrganization().getId() : null)
                .organizationName(profile.getOrganization() != null ? profile.getOrganization().getName() : null)
                .enabled(profile.isEnabled())
                .build();
    }
}
