package com.fittracker.workout.presentation.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record LogSetRequest(
        UUID exerciseId,
        String exerciseName,
        int setNumber,
        BigDecimal weightKg,
        int reps,
        Integer rir
) {
}
