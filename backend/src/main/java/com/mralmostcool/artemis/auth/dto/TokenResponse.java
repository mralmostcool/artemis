package com.mralmostcool.artemis.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken) {
}
