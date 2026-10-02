package com.fittracker.workout.service.implementation;

import com.fittracker.workout.persistence.entity.SessionExercise;
import com.fittracker.workout.persistence.entity.WorkoutRoutine;
import com.fittracker.workout.persistence.entity.WorkoutRoutineExercise;
import com.fittracker.workout.persistence.entity.WorkoutSession;
import com.fittracker.workout.persistence.entity.WorkoutSet;
import com.fittracker.workout.persistence.entity.enums.MuscleLabel;
import com.fittracker.workout.persistence.entity.enums.SessionType;
import com.fittracker.workout.persistence.repository.SessionExerciseRepository;
import com.fittracker.workout.persistence.repository.WorkoutRoutineRepository;
import com.fittracker.workout.persistence.repository.WorkoutSessionRepository;
import com.fittracker.workout.persistence.repository.WorkoutSetRepository;
import com.fittracker.workout.presentation.dto.AddSessionExerciseRequest;
import com.fittracker.workout.presentation.dto.LogSetRequest;
import com.fittracker.workout.presentation.dto.SessionExerciseDto;
import com.fittracker.workout.presentation.dto.SessionResponse;
import com.fittracker.workout.presentation.dto.SetResponse;
import com.fittracker.workout.presentation.dto.StartSessionRequest;
import com.fittracker.workout.service.exception.InvalidRoutineException;
import com.fittracker.workout.service.exception.InvalidSetDataException;
import com.fittracker.workout.service.exception.RoutineNotFoundException;
import com.fittracker.workout.service.exception.SessionNotFoundException;
import com.fittracker.workout.service.http.RoutineClient;
import com.fittracker.workout.service.interfaces.ProgressionService;
import com.fittracker.workout.service.interfaces.WorkoutSessionService;
import com.fittracker.workout.service.messaging.SessionEventProducer;
import com.fittracker.workout.utils.mapper.WorkoutMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkoutSessionServiceImpl implements WorkoutSessionService {

    private static final int DEFAULT_PLANNED_SETS = 3;

    private final WorkoutSessionRepository sessionRepository;
    private final WorkoutSetRepository setRepository;
    private final WorkoutRoutineRepository routineRepository;
    private final SessionExerciseRepository sessionExerciseRepository;
    private final WorkoutMapper mapper;
    private final ProgressionService progressionService;
    private final SessionEventProducer eventProducer;
    private final RoutineClient routineClient;

    @Override
    @Transactional
    public SessionResponse startSession(StartSessionRequest request, UUID userId) {
        WorkoutRoutine routine = null;
        if (request.routineId() != null) {
            routine = routineRepository.findById(request.routineId())
                    .filter(r -> r.getUserId().equals(userId))
                    .orElseThrow(() -> new RoutineNotFoundException(request.routineId()));
        }

        MuscleLabel muscleLabel = request.muscleLabel() != null
                ? request.muscleLabel()
                : (routine != null ? routine.getMuscleLabel() : null);

        SessionType sessionType = request.sessionType();
        if (sessionType == null && routine != null) {
            sessionType = routine.getSessionType();
        }
        if (sessionType == null) {
            sessionType = progressionService.getNextSessionType(userId, muscleLabel);
            log.info("Tipo de sesión auto-determinado: {} para muscleLabel: {}", sessionType, muscleLabel);
        }

        WorkoutSession session = mapper.toSessionEntity(request, userId);
        session.setMuscleLabel(muscleLabel);
        session.setSessionType(sessionType);

        if (routine != null) {
            session.setRoutineName(routine.getName());
            int position = 1;
            for (WorkoutRoutineExercise routineExercise : routine.getExercises()) {
                session.getExercises().add(mapper.toSessionExerciseEntity(
                        session,
                        routineExercise.getExerciseId(),
                        routineExercise.getExerciseName(),
                        position++,
                        routineExercise.getPlannedSets()));
            }
        }

        WorkoutSession saved = sessionRepository.save(session);
        log.info("Sesión creada: {} para usuario: {}{}",
                saved.getId(), userId,
                routine != null ? " desde rutina: " + routine.getName() : "");
        return mapper.toSessionResponse(saved);
    }

    @Override
    @Transactional
    public SetResponse logSet(UUID sessionId, LogSetRequest request) {
        WorkoutSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        if (request.weightKg() == null || request.weightKg().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidSetDataException("El peso debe ser mayor que 0");
        }
        if (request.reps() <= 0) {
            throw new InvalidSetDataException("Las repeticiones deben ser mayores que 0");
        }
        if (request.setNumber() <= 0) {
            throw new InvalidSetDataException("El número de serie debe ser mayor que 0");
        }

        BigDecimal volumeKg = request.weightKg().multiply(BigDecimal.valueOf(request.reps()));

        WorkoutSet workoutSet = WorkoutSet.builder()
                .session(session)
                .exerciseId(request.exerciseId())
                .exerciseName(request.exerciseName())
                .setNumber(request.setNumber())
                .weightKg(request.weightKg())
                .reps(request.reps())
                .rir(request.rir())
                .volumeKg(volumeKg)
                .build();

        WorkoutSet saved = setRepository.save(workoutSet);
        session.getSets().add(saved);

        log.info("Serie registrada: ejercicio={}, set={}, volumen={} para sesión={}",
                request.exerciseName(), request.setNumber(), volumeKg, sessionId);

        return mapper.toSetResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SessionResponse getSession(UUID sessionId) {
        WorkoutSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
        return mapper.toSessionResponse(session);
    }

    @Override
    @Transactional
    public void deleteSession(UUID sessionId) {
        WorkoutSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        eventProducer.sendSessionCompletedEvent(
                session.getId(),
                session.getUserId(),
                session.getMuscleLabel().name(),
                session.getSessionType().name()
        );

        sessionRepository.delete(session);
        log.info("Sesión eliminada: {}", sessionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionResponse> getSessions(UUID userId, LocalDate from, LocalDate to) {
        List<WorkoutSession> sessions;
        if (from != null && to != null) {
            sessions = sessionRepository.findByUserIdAndPerformedAtBetween(userId, from, to);
        } else {
            sessions = sessionRepository.findByUserIdOrderByPerformedAtDesc(userId);
        }
        return sessions.stream()
                .map(mapper::toSessionResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SessionExerciseDto addSessionExercise(UUID sessionId, AddSessionExerciseRequest request, UUID userId) {
        WorkoutSession session = sessionRepository.findById(sessionId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        Optional<SessionExercise> existing =
                sessionExerciseRepository.findBySessionIdAndExerciseId(sessionId, request.exerciseId());
        if (existing.isPresent()) {
            log.info("El ejercicio {} ya esta en la sesion {}; se devuelve sin duplicar",
                    request.exerciseId(), sessionId);
            return mapper.toSessionExerciseDto(existing.get());
        }

        int plannedSets = request.plannedSets() == null ? DEFAULT_PLANNED_SETS : request.plannedSets();
        if (plannedSets < 1) {
            throw new InvalidRoutineException("Las series planificadas deben ser al menos 1");
        }

        String exerciseName = resolveExerciseName(request.exerciseId());
        int nextPosition = session.getExercises().stream()
                .mapToInt(SessionExercise::getPosition)
                .max().orElse(0) + 1;

        SessionExercise exercise = mapper.toSessionExerciseEntity(
                session, request.exerciseId(), exerciseName, nextPosition, plannedSets);
        session.getExercises().add(exercise);
        SessionExercise saved = sessionExerciseRepository.save(exercise);

        log.info("Ejercicio agregado a la sesion: {} ({}), posicion: {}", exerciseName, sessionId, nextPosition);
        return mapper.toSessionExerciseDto(saved);
    }

    @Override
    @Transactional
    public void removeSessionExercise(UUID sessionId, UUID exerciseId, UUID userId) {
        WorkoutSession session = sessionRepository.findById(sessionId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        SessionExercise exercise = sessionExerciseRepository
                .findBySessionIdAndExerciseId(sessionId, exerciseId)
                .orElseThrow(() -> new SessionNotFoundException(
                        "Ejercicio no encontrado en la sesión: " + exerciseId));

        // orphanRemoval en workout_set elimina en cascada las series registradas
        int seriesBorradas = session.getSets().size();
        session.getSets().removeIf(set -> set.getExerciseId().equals(exerciseId));
        seriesBorradas -= session.getSets().size();

        session.getExercises().remove(exercise);
        log.info("Ejercicio {} eliminado de la sesion {} ({} series borradas)",
                exerciseId, sessionId, seriesBorradas);
    }

    private String resolveExerciseName(UUID exerciseId) {
        try {
            var exercise = routineClient.getExercise(exerciseId);
            if (exercise == null || exercise.name() == null || exercise.name().isBlank()) {
                throw new InvalidRoutineException("El ejercicio no existe en el catálogo: " + exerciseId);
            }
            return exercise.name();
        } catch (InvalidRoutineException e) {
            throw e;
        } catch (Exception e) {
            log.warn("No se pudo validar el ejercicio {} en routine-service: {}", exerciseId, e.getMessage());
            throw new InvalidRoutineException("No se pudo validar el ejercicio en el catálogo: " + exerciseId);
        }
    }
}
