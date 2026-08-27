package com.mralmostcool.artemis.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        UserResponse user) {
}
