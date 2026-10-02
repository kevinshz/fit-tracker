package com.fittracker.routine.presentation.dto;

import com.fittracker.routine.persistence.entity.enums.Equipment;
import com.fittracker.routine.persistence.entity.enums.MuscleGroup;

import java.util.UUID;

public record ExerciseSummaryDto(
        UUID id,
        String name,
        MuscleGroup primaryMuscle,
        Equipment equipment,
        boolean isCustom
) {
}
