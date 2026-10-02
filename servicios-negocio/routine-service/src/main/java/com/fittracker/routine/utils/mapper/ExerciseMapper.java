package com.fittracker.routine.utils.mapper;

import com.fittracker.routine.persistence.entity.Exercise;
import com.fittracker.routine.persistence.entity.enums.ProgressionStrategy;
import com.fittracker.routine.presentation.dto.ExerciseRequest;
import com.fittracker.routine.presentation.dto.ExerciseResponse;
import com.fittracker.routine.presentation.dto.ExerciseSummaryDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ExerciseMapper {

    public ExerciseResponse toResponse(Exercise exercise) {
        return new ExerciseResponse(
                exercise.getId(),
                exercise.getName(),
                exercise.getType(),
                exercise.getPrimaryMuscle(),
                List.copyOf(exercise.getSecondaryMuscles()),
                exercise.getEquipment(),
                exercise.getProgressionStrategy(),
                exercise.isCustom(),
                exercise.getCreatedBy(),
                exercise.getAliases().stream()
                        .map(alias -> alias.getAlias())
                        .collect(Collectors.toList())
        );
    }

    public ExerciseSummaryDto toSummary(Exercise exercise) {
        return new ExerciseSummaryDto(
                exercise.getId(),
                exercise.getName(),
                exercise.getPrimaryMuscle(),
                exercise.getEquipment(),
                exercise.isCustom()
        );
    }

    public void updateEntity(Exercise exercise, ExerciseRequest request) {
        exercise.setName(request.name());
        exercise.setType(request.type());
        exercise.setPrimaryMuscle(request.primaryMuscle());
        exercise.setSecondaryMuscles(
                request.secondaryMuscles() == null ? new ArrayList<>() : new ArrayList<>(request.secondaryMuscles())
        );
        exercise.setEquipment(request.equipment());
        if (request.progressionStrategy() != null) {
            exercise.setProgressionStrategy(request.progressionStrategy());
        }
    }

    public ProgressionStrategy defaultProgression() {
        return ProgressionStrategy.DOUBLE_PROGRESSION;
    }
}
