package com.mralmostcool.artemis.auth;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.mralmostcool.artemis.auth.dto.LoginRequest;
import com.mralmostcool.artemis.auth.dto.LoginResponse;
import com.mralmostcool.artemis.auth.dto.RefreshRequest;
import com.mralmostcool.artemis.auth.dto.RegisterRequest;
import com.mralmostcool.artemis.auth.dto.TokenResponse;
import com.mralmostcool.artemis.auth.dto.UserResponse;
import com.mralmostcool.artemis.auth.internal.token.JwtService;
import com.mralmostcool.artemis.auth.internal.token.Token;
import com.mralmostcool.artemis.auth.internal.token.TokenRepository;
import com.mralmostcool.artemis.auth.internal.user.User;
import com.mralmostcool.artemis.auth.internal.user.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${security.jwt.refresh-expiration}")
    private long refreshExpiration;

    public UserResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();

        User savedUser = userRepository.save(user);

        return new UserResponse(
                savedUser.getId(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getEmail(),
                savedUser.getCreatedAt());

    }

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities("ROLE_USER")
                .build();

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = generateAndSaveRefreshToken(user);

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
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (token.isRevoked() || token.getExpiryDate().isBefore(Instant.now())) {
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

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    public void logout(String refreshToken) {
        tokenRepository.findByToken(refreshToken).ifPresent(token -> {
            token.setRevoked(true);
            tokenRepository.save(token);
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
