package com.mralmostcool.artemis.auth;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.mralmostcool.artemis.auth.dto.AuthLogResponse;
import com.mralmostcool.artemis.auth.dto.LoginRequest;
import com.mralmostcool.artemis.auth.dto.LoginResponse;
import com.mralmostcool.artemis.auth.dto.RefreshRequest;
import com.mralmostcool.artemis.auth.dto.RegisterRequest;
import com.mralmostcool.artemis.auth.dto.TokenResponse;
import com.mralmostcool.artemis.auth.dto.UserResponse;
import com.mralmostcool.artemis.auth.internal.log.AuthLog;
import com.mralmostcool.artemis.auth.internal.log.AuthLogRepository;
import com.mralmostcool.artemis.auth.internal.token.JwtService;
import com.mralmostcool.artemis.auth.internal.token.Token;
import com.mralmostcool.artemis.auth.internal.token.TokenRepository;
import com.mralmostcool.artemis.auth.internal.user.User;
import com.mralmostcool.artemis.auth.internal.user.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final AuthLogRepository authLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final HttpServletRequest httpServletRequest;

    @Value("${security.jwt.refresh-expiration}")
    private long refreshExpiration;

    public UserResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            logEvent(null, "REGISTER", "FAILURE");
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();

        User savedUser = userRepository.save(user);
        logEvent(savedUser, "REGISTER", "SUCCESS");

        return new UserResponse(
                savedUser.getId(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getEmail(),
                savedUser.getCreatedAt());

    }

    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()
                    )
            );
        } catch (Exception e) {
            User user = userRepository.findByEmail(request.email()).orElse(null);
            logEvent(user, "LOGIN", "FAILURE");
            throw e;
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities("ROLE_USER")
                .build();

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = generateAndSaveRefreshToken(user);

        logEvent(user, "LOGIN", "SUCCESS");

        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getCreatedAt()
        );

        return new LoginResponse(accessToken, refreshToken, userResponse);
    }

    public TokenResponse refresh(RefreshRequest request) {
        String tokenStr = request.refreshToken();
        Token token = tokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> {
                    logEvent(null, "REFRESH", "FAILURE");
                    return new IllegalArgumentException("Invalid refresh token");
                });

        if (token.isRevoked() || token.getExpiryDate().isBefore(Instant.now())) {
            logEvent(token.getUser(), "REFRESH", "FAILURE");
            throw new IllegalArgumentException("Refresh token is expired or revoked");
        }

        token.setRevoked(true);
        tokenRepository.save(token);

        User user = token.getUser();
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities("ROLE_USER")
                .build();

        String newAccessToken = jwtService.generateToken(userDetails);
        String newRefreshToken = generateAndSaveRefreshToken(user);

        logEvent(user, "REFRESH", "SUCCESS");

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    public void logout(String refreshToken) {
        tokenRepository.findByToken(refreshToken).ifPresentOrElse(token -> {
            token.setRevoked(true);
            tokenRepository.save(token);
            logEvent(token.getUser(), "LOGOUT", "SUCCESS");
        }, () -> {
            logEvent(null, "LOGOUT", "FAILURE");
        });
    }

    public UserResponse getMe(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }

    public List<AuthLogResponse> getLogs(UUID userId) {
        return authLogRepository.findAllFiltered(userId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public Page<AuthLogResponse> getLogsPaginated(UUID userId, Pageable pageable) {
        return authLogRepository.findAllFiltered(userId, pageable)
                .map(this::mapToResponse);
    }

    public AuthLogResponse getLogById(UUID id) {
        return authLogRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new IllegalArgumentException("Auth log not found"));
    }

    private void logEvent(User user, String action, String status) {
        String ipAddress = null;
        String userAgent = null;
        try {
            if (httpServletRequest != null) {
                ipAddress = httpServletRequest.getHeader("X-Forwarded-For");
                if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                    ipAddress = httpServletRequest.getRemoteAddr();
                }
                userAgent = httpServletRequest.getHeader("User-Agent");
            }
        } catch (Exception e) {
            // Ignore if request context is not available
        }

        AuthLog authLog = AuthLog.builder()
                .user(user)
                .action(action)
                .status(status)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        authLogRepository.save(authLog);
    }

    private AuthLogResponse mapToResponse(AuthLog log) {
        return new AuthLogResponse(
                log.getId(),
                log.getUser() != null ? log.getUser().getId() : null,
                log.getAction(),
                log.getStatus(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getCreatedAt()
        );
    }

    private String generateAndSaveRefreshToken(User user) {
        String tokenString = java.util.UUID.randomUUID().toString();
        Token refreshToken = Token.builder()
                .user(user)
                .token(tokenString)
                .expiryDate(Instant.now().plusMillis(refreshExpiration))
                .revoked(false)
                .build();
        tokenRepository.save(refreshToken);
        return tokenString;
    }

}
