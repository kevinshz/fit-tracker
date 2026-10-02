package com.fittracker.workout.presentation.dto;

import com.fittracker.workout.persistence.entity.enums.MuscleLabel;
import com.fittracker.workout.persistence.entity.enums.SessionType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        LocalDate performedAt,
        SessionType sessionType,
        MuscleLabel muscleLabel,
        String notes,
        List<SetResponse> sets,
        LocalDateTime createdAt,
        List<SessionExerciseDto> exercises,
        String routineName
) {
}
