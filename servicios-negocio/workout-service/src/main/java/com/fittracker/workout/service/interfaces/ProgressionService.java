package com.fittracker.workout.service.interfaces;

import com.fittracker.workout.persistence.entity.enums.MuscleLabel;
import com.fittracker.workout.persistence.entity.enums.SessionType;
import com.fittracker.workout.presentation.dto.ProgressionRecommendation;

import java.util.UUID;

public interface ProgressionService {

    SessionType getNextSessionType(UUID userId, MuscleLabel muscleLabel);

    ProgressionRecommendation getRecommendation(UUID userId, MuscleLabel muscleLabel);
}
