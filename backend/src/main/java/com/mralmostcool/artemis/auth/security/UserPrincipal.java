package com.mralmostcool.artemis.auth.security;

import com.mralmostcool.artemis.auth.model.Profile;
import org.springframework.security.oauth2.jwt.Jwt;
import java.security.Principal;

public class UserPrincipal implements Principal {
    private final Jwt jwt;
    private final Profile profile;

    public UserPrincipal(Jwt jwt, Profile profile) {
        this.jwt = jwt;
        this.profile = profile;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public Profile getProfile() {
        return profile;
    }

    @Override
    public String getName() {
        return jwt.getSubject();
    }
}
