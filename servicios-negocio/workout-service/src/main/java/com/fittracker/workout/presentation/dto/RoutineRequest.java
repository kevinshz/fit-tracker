package com.fittracker.workout.presentation.dto;

import com.fittracker.workout.persistence.entity.enums.MuscleLabel;
import com.fittracker.workout.persistence.entity.enums.SessionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RoutineRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String name,

        SessionType sessionType,

        MuscleLabel muscleLabel,

        @Size(max = 500, message = "Las notas no pueden superar los 500 caracteres")
        String notes,

        @NotEmpty(message = "La rutina debe tener al menos un ejercicio")
        @Valid
        List<RoutineExerciseRequest> exercises
) {
}
