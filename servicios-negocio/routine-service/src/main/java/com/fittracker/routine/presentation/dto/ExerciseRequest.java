package com.fittracker.routine.presentation.dto;

import com.fittracker.routine.persistence.entity.enums.Equipment;
import com.fittracker.routine.persistence.entity.enums.ExerciseType;
import com.fittracker.routine.persistence.entity.enums.MuscleGroup;
import com.fittracker.routine.persistence.entity.enums.ProgressionStrategy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ExerciseRequest(
        @NotBlank(message = "el nombre es obligatorio")
        @Size(max = 200, message = "el nombre no puede superar 200 caracteres")
        String name,
        @NotNull(message = "el tipo es obligatorio")
        ExerciseType type,
        @NotNull(message = "el músculo principal es obligatorio")
        MuscleGroup primaryMuscle,
        List<MuscleGroup> secondaryMuscles,
        @NotNull(message = "el equipamiento es obligatorio")
        Equipment equipment,
        ProgressionStrategy progressionStrategy,
        List<@NotBlank @Size(max = 200, message = "los alias no pueden superar 200 caracteres") String> aliases
) {
}
