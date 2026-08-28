package com.mralmostcool.artemis.auth;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mralmostcool.artemis.auth.dto.AuthLogResponse;
import com.mralmostcool.artemis.auth.dto.LoginRequest;
import com.mralmostcool.artemis.auth.dto.LoginResponse;
import com.mralmostcool.artemis.auth.dto.RefreshRequest;
import com.mralmostcool.artemis.auth.dto.RegisterRequest;
import com.mralmostcool.artemis.auth.dto.TokenResponse;
import com.mralmostcool.artemis.auth.dto.UserResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        TokenResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserResponse response = authService.getMe(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/logs")
    public ResponseEntity<?> getLogs(
            @RequestParam(value = "userId", required = false) UUID userId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "direction", defaultValue = "DESC") String direction) {

        if (page != null && size != null) {
            Sort.Direction dir = Sort.Direction.fromString(direction.toUpperCase());
            Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));
            Page<AuthLogResponse> response = authService.getLogsPaginated(userId, pageable);
            return ResponseEntity.ok(response);
        } else {
            List<AuthLogResponse> response = authService.getLogs(userId);
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/logs/{id}")
    public ResponseEntity<AuthLogResponse> getLogById(@PathVariable("id") UUID id) {
        AuthLogResponse response = authService.getLogById(id);
        return ResponseEntity.ok(response);
    }

}
