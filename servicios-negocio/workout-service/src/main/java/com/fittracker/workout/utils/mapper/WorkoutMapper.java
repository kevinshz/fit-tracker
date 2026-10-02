package com.fittracker.workout.utils.mapper;

import com.fittracker.workout.persistence.entity.SessionExercise;
import com.fittracker.workout.persistence.entity.WorkoutRoutine;
import com.fittracker.workout.persistence.entity.WorkoutSession;
import com.fittracker.workout.persistence.entity.WorkoutSet;
import com.fittracker.workout.presentation.dto.RoutineExerciseResponse;
import com.fittracker.workout.presentation.dto.RoutineRequest;
import com.fittracker.workout.presentation.dto.RoutineResponse;
import com.fittracker.workout.presentation.dto.SessionExerciseDto;
import com.fittracker.workout.presentation.dto.SessionResponse;
import com.fittracker.workout.presentation.dto.SetResponse;
import com.fittracker.workout.presentation.dto.StartSessionRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class WorkoutMapper {

    public WorkoutSession toSessionEntity(StartSessionRequest request, java.util.UUID userId) {
        return WorkoutSession.builder()
                .userId(userId)
                .performedAt(request.performedAt())
                .sessionType(request.sessionType())
                .muscleLabel(request.muscleLabel())
                .notes(request.notes())
                .sets(new ArrayList<>())
                .exercises(new ArrayList<>())
                .build();
    }

    public SessionResponse toSessionResponse(WorkoutSession session) {
        List<SetResponse> setResponses = session.getSets() != null
                ? session.getSets().stream().map(this::toSetResponse).collect(Collectors.toList())
                : List.of();
        List<SessionExerciseDto> exerciseResponses = session.getExercises() != null
                ? session.getExercises().stream().map(this::toSessionExerciseDto).collect(Collectors.toList())
                : List.of();
        return new SessionResponse(
                session.getId(),
                session.getPerformedAt(),
                session.getSessionType(),
                session.getMuscleLabel(),
                session.getNotes(),
                setResponses,
                session.getCreatedAt(),
                exerciseResponses,
                session.getRoutineName()
        );
    }

    public SetResponse toSetResponse(WorkoutSet set) {
        return new SetResponse(
                set.getId(),
                set.getExerciseId(),
                set.getExerciseName(),
                set.getSetNumber(),
                set.getWeightKg(),
                set.getReps(),
                set.getRir(),
                set.getVolumeKg()
        );
    }

    public WorkoutRoutine toRoutineBase(RoutineRequest request, java.util.UUID userId) {
        return WorkoutRoutine.builder()
                .userId(userId)
                .name(request.name())
                .sessionType(request.sessionType())
                .muscleLabel(request.muscleLabel())
                .notes(request.notes())
                .exercises(new ArrayList<>())
                .build();
    }

    public RoutineResponse toRoutineResponse(WorkoutRoutine routine) {
        List<RoutineExerciseResponse> exercises = routine.getExercises() != null
                ? routine.getExercises().stream()
                    .map(ex -> new RoutineExerciseResponse(
                            ex.getId(), ex.getExerciseId(), ex.getExerciseName(),
                            ex.getPosition(), ex.getPlannedSets()))
                    .collect(Collectors.toList())
                : List.of();
        return new RoutineResponse(
                routine.getId(),
                routine.getName(),
                routine.getSessionType(),
                routine.getMuscleLabel(),
                routine.getNotes(),
                exercises,
                routine.getCreatedAt(),
                routine.getUpdatedAt()
        );
    }

    public SessionExercise toSessionExerciseEntity(WorkoutSession session, java.util.UUID exerciseId,
                                                   String exerciseName, int position, int plannedSets) {
        return SessionExercise.builder()
                .session(session)
                .exerciseId(exerciseId)
                .exerciseName(exerciseName)
                .position(position)
                .plannedSets(plannedSets)
                .build();
    }

    public SessionExerciseDto toSessionExerciseDto(SessionExercise exercise) {
        return new SessionExerciseDto(
                exercise.getId(),
                exercise.getExerciseId(),
                exercise.getExerciseName(),
                exercise.getPosition(),
                exercise.getPlannedSets()
        );
    }
}
