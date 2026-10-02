package com.fittracker.workout.presentation.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RoutineExerciseRequest(
        @NotNull(message = "El ejercicio es obligatorio")
        UUID exerciseId,

        Integer plannedSets
) {
}
