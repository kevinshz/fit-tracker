package com.fittracker.routine.presentation.dto;

import com.fittracker.routine.persistence.entity.enums.Equipment;
import com.fittracker.routine.persistence.entity.enums.ExerciseType;
import com.fittracker.routine.persistence.entity.enums.MuscleGroup;
import com.fittracker.routine.persistence.entity.enums.ProgressionStrategy;

import java.util.List;
import java.util.UUID;

public record ExerciseResponse(
        UUID id,
        String name,
        ExerciseType type,
        MuscleGroup primaryMuscle,
        List<MuscleGroup> secondaryMuscles,
        Equipment equipment,
        ProgressionStrategy progressionStrategy,
        boolean isCustom,
        String createdBy,
        List<String> aliases
) {
}
