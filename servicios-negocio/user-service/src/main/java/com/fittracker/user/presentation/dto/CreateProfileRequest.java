package com.fittracker.user.presentation.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.UUID;

public record CreateProfileRequest(
        UUID userId,
        @NotBlank String email,
        @NotBlank String name,
        Double bodyWeight,
        Double height,
        LocalDate dateOfBirth,
        String gender,
        String fitnessGoal,
        String experienceLevel
) {
}
