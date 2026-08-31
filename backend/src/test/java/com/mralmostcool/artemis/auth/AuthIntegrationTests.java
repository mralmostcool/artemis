package com.mralmostcool.artemis.auth;

import com.mralmostcool.artemis.auth.internal.model.Organization;
import com.mralmostcool.artemis.auth.internal.model.Profile;
import com.mralmostcool.artemis.auth.internal.model.Role;
import com.mralmostcool.artemis.auth.internal.repository.OrganizationRepository;
import com.mralmostcool.artemis.auth.internal.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AuthIntegrationTests {

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Test
    @Transactional
    void testCreateProfileWithEnrichedFields() {
        Organization organization = Organization.builder()
                .name("Test Shipping Co")
                .build();
        organization = organizationRepository.save(organization);

        UUID profileId = UUID.randomUUID();
        Profile profile = Profile.builder()
                .id(profileId)
                .email("test@example.com")
                .role(Role.COMPANY_ADMIN)
                .firstName("John")
                .lastName("Doe")
                .displayName("John Doe")
                .gender("MALE")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .organization(organization)
                .enabled(true)
                .build();

        Profile saved = profileRepository.save(profile);
        assertThat(saved.getId()).isEqualTo(profileId);
        assertThat(saved.getFirstName()).isEqualTo("John");
        assertThat(saved.getLastName()).isEqualTo("Doe");
        assertThat(saved.getGender()).isEqualTo("MALE");
        assertThat(saved.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(saved.getOrganization().getName()).isEqualTo("Test Shipping Co");
    }
}
