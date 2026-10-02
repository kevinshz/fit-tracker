package com.fittracker.workout.presentation.dto;

import com.fittracker.workout.persistence.entity.enums.MuscleLabel;
import com.fittracker.workout.persistence.entity.enums.SessionType;

import java.util.List;

public record ProgressionRecommendation(
        SessionType nextSessionType,
        MuscleLabel muscleLabel,
        List<ExerciseRecommendation> items
) {
}
