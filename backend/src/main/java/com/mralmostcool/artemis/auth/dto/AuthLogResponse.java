package com.mralmostcool.artemis.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record AuthLogResponse(
        UUID id,
        UUID userId,
        String action,
        String status,
        String ipAddress,
        String userAgent,
        Instant createdAt) {
}
