package com.fittracker.workout.presentation.dto;

import com.fittracker.workout.persistence.entity.enums.MuscleLabel;
import com.fittracker.workout.persistence.entity.enums.SessionType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RoutineResponse(
        UUID id,
        String name,
        SessionType sessionType,
        MuscleLabel muscleLabel,
        String notes,
        List<RoutineExerciseResponse> exercises,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
