package com.fittracker.user.presentation.dto;

import java.time.LocalDate;

public record UpdateProfileRequest(
        String name,
        Double bodyWeight,
        Double height,
        LocalDate dateOfBirth,
        String gender,
        String fitnessGoal,
        String experienceLevel
) {
}
