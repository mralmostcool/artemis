package com.mralmostcool.artemis.auth;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.test.ApplicationModuleTest;

@ApplicationModuleTest
class AuthModuleTest {

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private org.springframework.security.authentication.AuthenticationManager authenticationManager;


    @Test
    void verifiesModule() {
    }
}
