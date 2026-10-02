package com.fittracker.workout.service.implementation;

import com.fittracker.workout.persistence.entity.WorkoutRoutine;
import com.fittracker.workout.persistence.entity.WorkoutRoutineExercise;
import com.fittracker.workout.persistence.repository.WorkoutRoutineRepository;
import com.fittracker.workout.presentation.dto.RoutineExerciseRequest;
import com.fittracker.workout.presentation.dto.RoutineRequest;
import com.fittracker.workout.presentation.dto.RoutineResponse;
import com.fittracker.workout.service.exception.InvalidRoutineException;
import com.fittracker.workout.service.exception.RoutineNotFoundException;
import com.fittracker.workout.service.http.RoutineClient;
import com.fittracker.workout.service.interfaces.RoutineService;
import com.fittracker.workout.utils.mapper.WorkoutMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoutineServiceImpl implements RoutineService {

    private static final int DEFAULT_PLANNED_SETS = 3;

    private final WorkoutRoutineRepository routineRepository;
    private final WorkoutMapper mapper;
    private final RoutineClient routineClient;

    @Override
    @Transactional
    public RoutineResponse createRoutine(RoutineRequest request, UUID userId) {
        validateRoutine(request);

        WorkoutRoutine routine = mapper.toRoutineBase(request, userId);
        applyExercises(routine, request);
        WorkoutRoutine saved = routineRepository.save(routine);

        log.info("Rutina creada: {} con {} ejercicios para usuario: {}",
                saved.getId(), saved.getExercises().size(), userId);
        return mapper.toRoutineResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoutineResponse> listRoutines(UUID userId) {
        return routineRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(mapper::toRoutineResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RoutineResponse getRoutine(UUID routineId, UUID userId) {
        return mapper.toRoutineResponse(getOwnedRoutine(routineId, userId));
    }

    @Override
    @Transactional
    public RoutineResponse updateRoutine(UUID routineId, RoutineRequest request, UUID userId) {
        validateRoutine(request);

        WorkoutRoutine routine = getOwnedRoutine(routineId, userId);
        routine.setName(request.name());
        routine.setSessionType(request.sessionType());
        routine.setMuscleLabel(request.muscleLabel());
        routine.setNotes(request.notes());
        applyExercises(routine, request);

        WorkoutRoutine saved = routineRepository.save(routine);
        log.info("Rutina actualizada: {} para usuario: {}", saved.getId(), userId);
        return mapper.toRoutineResponse(saved);
    }

    @Override
    @Transactional
    public void deleteRoutine(UUID routineId, UUID userId) {
        WorkoutRoutine routine = getOwnedRoutine(routineId, userId);
        routineRepository.delete(routine);
        log.info("Rutina eliminada: {} para usuario: {}", routineId, userId);
    }

    private WorkoutRoutine getOwnedRoutine(UUID routineId, UUID userId) {
        return routineRepository.findById(routineId)
                .filter(routine -> routine.getUserId().equals(userId))
                .orElseThrow(() -> new RoutineNotFoundException(routineId));
    }

    private void validateRoutine(RoutineRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new InvalidRoutineException("El nombre de la rutina es obligatorio");
        }
        if (request.exercises() == null || request.exercises().isEmpty()) {
            throw new InvalidRoutineException("La rutina debe tener al menos un ejercicio");
        }
        for (RoutineExerciseRequest exercise : request.exercises()) {
            if (exercise.exerciseId() == null) {
                throw new InvalidRoutineException("Todos los ejercicios deben tener un id valido");
            }
            if (exercise.plannedSets() != null && exercise.plannedSets() < 1) {
                throw new InvalidRoutineException("Las series planificadas deben ser al menos 1");
            }
        }
    }

    private void applyExercises(WorkoutRoutine routine, RoutineRequest request) {
        routine.getExercises().clear();
        int position = 1;
        for (RoutineExerciseRequest exercise : request.exercises()) {
            routine.getExercises().add(WorkoutRoutineExercise.builder()
                    .routine(routine)
                    .exerciseId(exercise.exerciseId())
                    .exerciseName(resolveExerciseName(exercise.exerciseId()))
                    .position(position++)
                    .plannedSets(exercise.plannedSets() == null
                            ? DEFAULT_PLANNED_SETS
                            : exercise.plannedSets())
                    .build());
        }
    }

    private String resolveExerciseName(UUID exerciseId) {
        try {
            var exercise = routineClient.getExercise(exerciseId);
            if (exercise == null || exercise.name() == null || exercise.name().isBlank()) {
                throw new InvalidRoutineException("El ejercicio no existe en el catalogo: " + exerciseId);
            }
            return exercise.name();
        } catch (InvalidRoutineException e) {
            throw e;
        } catch (Exception e) {
            log.warn("No se pudo validar el ejercicio {} en routine-service: {}", exerciseId, e.getMessage());
            throw new InvalidRoutineException("No se pudo validar el ejercicio en el catalogo: " + exerciseId);
        }
    }
}
