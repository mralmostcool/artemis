package com.mralmostcool.artemis.auth;

import com.mralmostcool.artemis.auth.internal.dto.ProfileRequest;
import com.mralmostcool.artemis.auth.internal.dto.ProfileResponse;
import com.mralmostcool.artemis.auth.internal.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        ProfileResponse response = authService.getProfile(principal);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ProfileResponse> registerProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody ProfileRequest request) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            ProfileResponse response = authService.registerProfile(principal, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping
    public ResponseEntity<ProfileResponse> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody ProfileRequest request) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            ProfileResponse response = authService.updateProfile(principal, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/users/{userId}/status")
    public ResponseEntity<ProfileResponse> updateUserStatus(
            @AuthenticationPrincipal UserPrincipal adminPrincipal,
            @PathVariable UUID userId,
            @RequestParam boolean enabled) {

        if (adminPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            ProfileResponse response = authService.updateUserStatus(adminPrincipal, userId, enabled);
            return ResponseEntity.ok(response);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
