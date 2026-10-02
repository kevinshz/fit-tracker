package com.fittracker.workout.presentation.dto;

import java.util.UUID;

public record RoutineExerciseResponse(
        UUID id,
        UUID exerciseId,
        String exerciseName,
        int position,
        int plannedSets
) {
}
