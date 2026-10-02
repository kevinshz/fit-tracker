package com.fittracker.messaging.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record WorkoutSessionCompletedEvent(
        UUID sessionId,
        UUID userId,
        String muscleLabel,
        LocalDateTime completedAt
) {
}
