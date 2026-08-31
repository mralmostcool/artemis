package com.mralmostcool.artemis.auth.internal.security;

import com.mralmostcool.artemis.auth.internal.model.Profile;
import com.mralmostcool.artemis.auth.internal.repository.ProfileRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

@Component
public class SupabaseJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final ProfileRepository profileRepository;

    public SupabaseJwtAuthenticationConverter(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        Profile profile = profileRepository.findById(userId).orElse(null);

        Collection<GrantedAuthority> authorities = new ArrayList<>();
        if (profile != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + profile.getRole().name()));
        }

        UserPrincipal principal = new UserPrincipal(jwt, profile);
        return new UsernamePasswordAuthenticationToken(principal, jwt, authorities);
    }
}
