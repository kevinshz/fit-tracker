package com.fittracker.workout.service.http.dto;

import java.util.UUID;

public record ExerciseSummaryDto(
        UUID id,
        String name,
        String primaryMuscle,
        String equipment
) {
}
