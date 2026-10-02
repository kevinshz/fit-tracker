package com.fittracker.workout.presentation.dto;

import com.fittracker.workout.persistence.entity.enums.MuscleLabel;
import com.fittracker.workout.persistence.entity.enums.SessionType;

import java.time.LocalDate;

public record StartSessionRequest(
        LocalDate performedAt,
        MuscleLabel muscleLabel,
        SessionType sessionType,
        String notes,
        java.util.UUID routineId
) {
}
