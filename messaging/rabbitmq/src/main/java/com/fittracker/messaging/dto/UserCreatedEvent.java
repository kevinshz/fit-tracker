package com.fittracker.messaging.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserCreatedEvent(
        UUID userId,
        String email,
        String name,
        LocalDateTime createdAt
) {
}
