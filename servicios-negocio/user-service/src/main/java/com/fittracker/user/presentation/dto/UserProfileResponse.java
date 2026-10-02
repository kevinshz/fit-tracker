package com.fittracker.user.presentation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        UUID userId,
        String email,
        String name,
        Double bodyWeight,
        Double height,
        LocalDate dateOfBirth,
        String gender,
        String fitnessGoal,
        String experienceLevel,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
