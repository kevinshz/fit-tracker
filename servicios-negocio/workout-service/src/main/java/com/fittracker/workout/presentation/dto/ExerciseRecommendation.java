package com.fittracker.workout.presentation.dto;

import com.fittracker.workout.persistence.entity.enums.ProgressionAction;

import java.math.BigDecimal;
import java.util.UUID;

public record ExerciseRecommendation(
        UUID exerciseId,
        String exerciseName,
        ProgressionAction action,
        BigDecimal currentLoad,
        BigDecimal suggestedLoad,
        String reason
) {
}
