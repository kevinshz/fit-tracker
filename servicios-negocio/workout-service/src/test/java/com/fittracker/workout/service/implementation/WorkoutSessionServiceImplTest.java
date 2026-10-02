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
import com.fittracker.workout.presentation.dto.StartSessionRequest;
import com.fittracker.workout.service.exception.InvalidRoutineException;
import com.fittracker.workout.service.exception.InvalidSetDataException;
import com.fittracker.workout.service.exception.RoutineNotFoundException;
import com.fittracker.workout.service.exception.SessionNotFoundException;
import com.fittracker.workout.service.http.RoutineClient;
import com.fittracker.workout.service.http.dto.ExerciseSummaryDto;
import com.fittracker.workout.service.interfaces.ProgressionService;
import com.fittracker.workout.service.messaging.SessionEventProducer;
import com.fittracker.workout.utils.mapper.WorkoutMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkoutSessionServiceImpl - Tests Unitarios")
class WorkoutSessionServiceImplTest {

    @Mock private WorkoutSessionRepository sessionRepository;
    @Mock private WorkoutSetRepository setRepository;
    @Mock private WorkoutRoutineRepository routineRepository;
    @Mock private SessionExerciseRepository sessionExerciseRepository;
    @Spy private WorkoutMapper mapper = new WorkoutMapper();
    @Mock private ProgressionService progressionService;
    @Mock private SessionEventProducer eventProducer;
    @Mock private RoutineClient routineClient;

    @InjectMocks private WorkoutSessionServiceImpl service;

    private final UUID userId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();

    private WorkoutSession buildSession() {
        return WorkoutSession.builder()
                .id(sessionId)
                .userId(userId)
                .performedAt(LocalDate.now())
                .sessionType(SessionType.HYPERTROPHY)
                .muscleLabel(MuscleLabel.CHEST_BACK)
                .notes("entrenamiento")
                .sets(new ArrayList<>())
                .build();
    }

    @Nested
    @DisplayName("startSession")
    class StartSession {

        @Test
        @DisplayName("Con sessionType explicito no consulta progressionService")
        void explicitType_skipsProgression() {
            var request = new StartSessionRequest(LocalDate.now(), MuscleLabel.LEGS, SessionType.STRENGTH, "notas", null);
            when(sessionRepository.save(any(WorkoutSession.class))).thenAnswer(inv -> {
                WorkoutSession s = inv.getArgument(0);
                s.setId(sessionId);
                return s;
            });

            var response = service.startSession(request, userId);

            assertThat(response.id()).isEqualTo(sessionId);
            assertThat(response.sessionType()).isEqualTo(SessionType.STRENGTH);
            verify(progressionService, never()).getNextSessionType(any(), any());
        }

        @Test
        @DisplayName("Con sessionType null auto-determina via progressionService")
        void nullType_autoDetermines() {
            var request = new StartSessionRequest(LocalDate.now(), MuscleLabel.LEGS, null, null, null);
            when(progressionService.getNextSessionType(userId, MuscleLabel.LEGS))
                    .thenReturn(SessionType.STRENGTH);
            when(sessionRepository.save(any(WorkoutSession.class))).thenAnswer(inv -> {
                WorkoutSession s = inv.getArgument(0);
                s.setId(sessionId);
                return s;
            });

            var response = service.startSession(request, userId);

            assertThat(response.sessionType()).isEqualTo(SessionType.STRENGTH);
            verify(progressionService).getNextSessionType(userId, MuscleLabel.LEGS);
        }
    }

    @Nested
    @DisplayName("logSet")
    class LogSet {

        @Test
        @DisplayName("Sesion inexistente lanza SessionNotFoundException")
        void sessionNotFound_throws() {
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());
            var request = new LogSetRequest(UUID.randomUUID(), "Press banca", 1, new BigDecimal("60"), 8, 2);

            assertThatThrownBy(() -> service.logSet(sessionId, request))
                    .isInstanceOf(SessionNotFoundException.class);
        }

        @Test
        @DisplayName("Peso nulo lanza InvalidSetDataException")
        void nullWeight_throws() {
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(buildSession()));
            var request = new LogSetRequest(UUID.randomUUID(), "Press banca", 1, null, 8, 2);

            assertThatThrownBy(() -> service.logSet(sessionId, request))
                    .isInstanceOf(InvalidSetDataException.class)
                    .hasMessageContaining("peso");
        }

        @Test
        @DisplayName("Peso negativo lanza InvalidSetDataException")
        void negativeWeight_throws() {
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(buildSession()));
            var request = new LogSetRequest(UUID.randomUUID(), "Press banca", 1, BigDecimal.valueOf(-5), 8, 2);

            assertThatThrownBy(() -> service.logSet(sessionId, request))
                    .isInstanceOf(InvalidSetDataException.class);
        }

        @Test
        @DisplayName("Reps 0 lanza InvalidSetDataException")
        void zeroReps_throws() {
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(buildSession()));
            var request = new LogSetRequest(UUID.randomUUID(), "Press banca", 1, new BigDecimal("60"), 0, 2);

            assertThatThrownBy(() -> service.logSet(sessionId, request))
                    .isInstanceOf(InvalidSetDataException.class)
                    .hasMessageContaining("repeticiones");
        }

        @Test
        @DisplayName("setNumber 0 lanza InvalidSetDataException")
        void zeroSetNumber_throws() {
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(buildSession()));
            var request = new LogSetRequest(UUID.randomUUID(), "Press banca", 0, new BigDecimal("60"), 8, 2);

            assertThatThrownBy(() -> service.logSet(sessionId, request))
                    .isInstanceOf(InvalidSetDataException.class)
                    .hasMessageContaining("serie");
        }

        @Test
        @DisplayName("Happy path: calcula volumen = peso x reps y guarda")
        void happyPath_computesVolume() {
            WorkoutSession session = buildSession();
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(setRepository.save(any(WorkoutSet.class))).thenAnswer(inv -> {
                WorkoutSet s = inv.getArgument(0);
                s.setId(UUID.randomUUID());
                return s;
            });
            var request = new LogSetRequest(UUID.randomUUID(), "Press banca", 2, new BigDecimal("60.5"), 8, 3);

            var response = service.logSet(sessionId, request);

            assertThat(response.volumeKg()).isEqualByComparingTo("484.0");
            assertThat(response.weightKg()).isEqualByComparingTo("60.5");
            assertThat(response.reps()).isEqualTo(8);
            assertThat(response.rir()).isEqualTo(3);
            assertThat(session.getSets()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getSession")
    class GetSession {

        @Test
        @DisplayName("Retorna la sesion con sus sets")
        void found_returnsSession() {
            WorkoutSession session = buildSession();
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

            var response = service.getSession(sessionId);

            assertThat(response.id()).isEqualTo(sessionId);
            assertThat(response.muscleLabel()).isEqualTo(MuscleLabel.CHEST_BACK);
            assertThat(response.sets()).isEmpty();
        }

        @Test
        @DisplayName("No encontrada lanza SessionNotFoundException")
        void notFound_throws() {
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getSession(sessionId))
                    .isInstanceOf(SessionNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteSession")
    class DeleteSession {

        @Test
        @DisplayName("Envia evento SESSION_COMPLETED y elimina")
        void sendsEventAndDeletes() {
            WorkoutSession session = buildSession();
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

            service.deleteSession(sessionId);

            verify(eventProducer).sendSessionCompletedEvent(
                    sessionId, userId, MuscleLabel.CHEST_BACK.name(), SessionType.HYPERTROPHY.name());
            verify(sessionRepository).delete(session);
        }

        @Test
        @DisplayName("Sesion inexistente lanza SessionNotFoundException y no envia evento")
        void notFound_throws() {
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteSession(sessionId))
                    .isInstanceOf(SessionNotFoundException.class);
            verify(eventProducer, never()).sendSessionCompletedEvent(any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("getSessions")
    class GetSessions {

        @Test
        @DisplayName("Sin fechas busca por usuario ordenado desc")
        void noDates_queriesByUser() {
            when(sessionRepository.findByUserIdOrderByPerformedAtDesc(userId))
                    .thenReturn(List.of(buildSession()));

            var result = service.getSessions(userId, null, null);

            assertThat(result).hasSize(1);
            verify(sessionRepository).findByUserIdOrderByPerformedAtDesc(userId);
            verify(sessionRepository, never()).findByUserIdAndPerformedAtBetween(any(), any(), any());
        }

        @Test
        @DisplayName("Con fechas busca por rango")
        void withDates_queriesByRange() {
            LocalDate from = LocalDate.now().minusDays(7);
            LocalDate to = LocalDate.now();
            when(sessionRepository.findByUserIdAndPerformedAtBetween(userId, from, to))
                    .thenReturn(List.of(buildSession()));

            var result = service.getSessions(userId, from, to);

            assertThat(result).hasSize(1);
            verify(sessionRepository).findByUserIdAndPerformedAtBetween(userId, from, to);
        }
    }

    @Nested
    @DisplayName("startSession con rutina")
    class StartSessionWithRoutine {

        private WorkoutRoutine buildRoutine() {
            WorkoutRoutine routine = WorkoutRoutine.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .name("Día de Pecho")
                    .sessionType(SessionType.STRENGTH)
                    .muscleLabel(MuscleLabel.CHEST_BACK)
                    .exercises(new ArrayList<>())
                    .build();
            routine.getExercises().add(WorkoutRoutineExercise.builder()
                    .routine(routine)
                    .exerciseId(UUID.randomUUID())
                    .exerciseName("Press banca")
                    .position(1)
                    .plannedSets(4)
                    .build());
            routine.getExercises().add(WorkoutRoutineExercise.builder()
                    .routine(routine)
                    .exerciseId(UUID.randomUUID())
                    .exerciseName("Aperturas con mancuernas")
                    .position(2)
                    .plannedSets(3)
                    .build());
            return routine;
        }

        @Test
        @DisplayName("Copia ejercicios planificados, posiciones y nombre de rutina")
        void copiesRoutineExercises() {
            WorkoutRoutine routine = buildRoutine();
            when(routineRepository.findById(routine.getId())).thenReturn(Optional.of(routine));
            when(sessionRepository.save(any(WorkoutSession.class))).thenAnswer(inv -> {
                WorkoutSession s = inv.getArgument(0);
                s.setId(sessionId);
                return s;
            });
            var request = new StartSessionRequest(LocalDate.now(), MuscleLabel.CHEST_BACK,
                    SessionType.STRENGTH, null, routine.getId());

            var response = service.startSession(request, userId);

            assertThat(response.routineName()).isEqualTo("Día de Pecho");
            assertThat(response.exercises()).hasSize(2);
            assertThat(response.exercises().get(0).exerciseName()).isEqualTo("Press banca");
            assertThat(response.exercises().get(0).plannedSets()).isEqualTo(4);
            assertThat(response.exercises().get(0).position()).isEqualTo(1);
            assertThat(response.exercises().get(1).position()).isEqualTo(2);
        }

        @Test
        @DisplayName("Rutina de otro usuario lanza RoutineNotFoundException")
        void foreignRoutine_throws() {
            WorkoutRoutine routine = buildRoutine();
            routine.setUserId(UUID.randomUUID());
            when(routineRepository.findById(routine.getId())).thenReturn(Optional.of(routine));
            var request = new StartSessionRequest(LocalDate.now(), MuscleLabel.CHEST_BACK,
                    SessionType.STRENGTH, null, routine.getId());

            assertThatThrownBy(() -> service.startSession(request, userId))
                    .isInstanceOf(RoutineNotFoundException.class);
            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Sin sessionType usa el de la rutina en vez de progressionService")
        void fallbackToRoutineType() {
            WorkoutRoutine routine = buildRoutine();
            when(routineRepository.findById(routine.getId())).thenReturn(Optional.of(routine));
            when(sessionRepository.save(any(WorkoutSession.class))).thenAnswer(inv -> {
                WorkoutSession s = inv.getArgument(0);
                s.setId(sessionId);
                return s;
            });
            var request = new StartSessionRequest(LocalDate.now(), MuscleLabel.CHEST_BACK,
                    null, null, routine.getId());

            var response = service.startSession(request, userId);

            assertThat(response.sessionType()).isEqualTo(SessionType.STRENGTH);
            verify(progressionService, never()).getNextSessionType(any(), any());
        }
    }

    @Nested
    @DisplayName("addSessionExercise")
    class AddSessionExercise {

        private final UUID exerciseId = UUID.randomUUID();

        @Test
        @DisplayName("Agrega ejercicio con nombre resuelto del catálogo y 3 series por defecto")
        void addsExercise_resolvesName() {
            WorkoutSession session = buildSession();
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(sessionExerciseRepository.findBySessionIdAndExerciseId(sessionId, exerciseId))
                    .thenReturn(Optional.empty());
            when(routineClient.getExercise(exerciseId))
                    .thenReturn(new ExerciseSummaryDto(exerciseId, "Press banca", "CHEST", "BARBELL"));
            when(sessionExerciseRepository.save(any(SessionExercise.class))).thenAnswer(inv -> {
                SessionExercise e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return e;
            });
            var request = new AddSessionExerciseRequest(exerciseId, null);

            var response = service.addSessionExercise(sessionId, request, userId);

            assertThat(response.exerciseName()).isEqualTo("Press banca");
            assertThat(response.plannedSets()).isEqualTo(3);
            assertThat(response.position()).isEqualTo(1);
            assertThat(session.getExercises()).hasSize(1);
        }

        @Test
        @DisplayName("Serie planificada explicita se respeta")
        void explicitPlannedSets() {
            WorkoutSession session = buildSession();
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(sessionExerciseRepository.findBySessionIdAndExerciseId(sessionId, exerciseId))
                    .thenReturn(Optional.empty());
            when(routineClient.getExercise(exerciseId))
                    .thenReturn(new ExerciseSummaryDto(exerciseId, "Press banca", "CHEST", "BARBELL"));
            when(sessionExerciseRepository.save(any(SessionExercise.class))).thenAnswer(inv -> {
                SessionExercise e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return e;
            });
            var request = new AddSessionExerciseRequest(exerciseId, 5);

            var response = service.addSessionExercise(sessionId, request, userId);

            assertThat(response.plannedSets()).isEqualTo(5);
        }

        @Test
        @DisplayName("plannedSets < 1 lanza InvalidRoutineException")
        void invalidPlannedSets_throws() {
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(buildSession()));
            when(sessionExerciseRepository.findBySessionIdAndExerciseId(sessionId, exerciseId))
                    .thenReturn(Optional.empty());
            var request = new AddSessionExerciseRequest(exerciseId, 0);

            assertThatThrownBy(() -> service.addSessionExercise(sessionId, request, userId))
                    .isInstanceOf(InvalidRoutineException.class)
                    .hasMessageContaining("series");
        }

        @Test
        @DisplayName("Ejercicio ya presente se devuelve sin duplicar")
        void duplicate_returnsExisting() {
            WorkoutSession session = buildSession();
            SessionExercise existing = SessionExercise.builder()
                    .id(UUID.randomUUID())
                    .session(session)
                    .exerciseId(exerciseId)
                    .exerciseName("Press banca")
                    .position(1)
                    .plannedSets(3)
                    .build();
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(sessionExerciseRepository.findBySessionIdAndExerciseId(sessionId, exerciseId))
                    .thenReturn(Optional.of(existing));
            var request = new AddSessionExerciseRequest(exerciseId, null);

            var response = service.addSessionExercise(sessionId, request, userId);

            assertThat(response.id()).isEqualTo(existing.getId());
            verify(sessionExerciseRepository, never()).save(any());
            verify(routineClient, never()).getExercise(any());
        }

        @Test
        @DisplayName("Sesión inexistente lanza SessionNotFoundException")
        void sessionNotFound_throws() {
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());
            var request = new AddSessionExerciseRequest(exerciseId, null);

            assertThatThrownBy(() -> service.addSessionExercise(sessionId, request, userId))
                    .isInstanceOf(SessionNotFoundException.class);
        }

        @Test
        @DisplayName("Sesión de otro usuario lanza SessionNotFoundException")
        void foreignSession_throws() {
            WorkoutSession session = buildSession();
            session.setUserId(UUID.randomUUID());
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            var request = new AddSessionExerciseRequest(exerciseId, null);

            assertThatThrownBy(() -> service.addSessionExercise(sessionId, request, userId))
                    .isInstanceOf(SessionNotFoundException.class);
        }

        @Test
        @DisplayName("Catálogo no disponible lanza InvalidRoutineException")
        void catalogDown_throws() {
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(buildSession()));
            when(sessionExerciseRepository.findBySessionIdAndExerciseId(sessionId, exerciseId))
                    .thenReturn(Optional.empty());
            when(routineClient.getExercise(exerciseId)).thenThrow(new RuntimeException("caído"));

            var request = new AddSessionExerciseRequest(exerciseId, null);

            assertThatThrownBy(() -> service.addSessionExercise(sessionId, request, userId))
                    .isInstanceOf(InvalidRoutineException.class);
        }
    }

    @Nested
    @DisplayName("removeSessionExercise")
    class RemoveSessionExercise {

        private final UUID exerciseId = UUID.randomUUID();

        @Test
        @DisplayName("Elimina el ejercicio y sus series registradas")
        void removesExerciseAndSets() {
            WorkoutSession session = buildSession();
            UUID otroExerciseId = UUID.randomUUID();
            session.getSets().add(WorkoutSet.builder()
                    .id(UUID.randomUUID()).session(session)
                    .exerciseId(exerciseId).exerciseName("Press banca")
                    .setNumber(1).weightKg(new BigDecimal("60")).reps(8)
                    .volumeKg(new BigDecimal("480")).build());
            session.getSets().add(WorkoutSet.builder()
                    .id(UUID.randomUUID()).session(session)
                    .exerciseId(otroExerciseId).exerciseName("Sentadilla")
                    .setNumber(1).weightKg(new BigDecimal("80")).reps(5)
                    .volumeKg(new BigDecimal("400")).build());
            SessionExercise exercise = SessionExercise.builder()
                    .id(UUID.randomUUID()).session(session)
                    .exerciseId(exerciseId).exerciseName("Press banca")
                    .position(1).plannedSets(3).build();
            session.getExercises().add(exercise);
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(sessionExerciseRepository.findBySessionIdAndExerciseId(sessionId, exerciseId))
                    .thenReturn(Optional.of(exercise));

            service.removeSessionExercise(sessionId, exerciseId, userId);

            assertThat(session.getExercises()).isEmpty();
            assertThat(session.getSets()).hasSize(1);
            assertThat(session.getSets().get(0).getExerciseId()).isEqualTo(otroExerciseId);
        }

        @Test
        @DisplayName("Ejercicio no presente lanza SessionNotFoundException")
        void exerciseNotInSession_throws() {
            WorkoutSession session = buildSession();
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(sessionExerciseRepository.findBySessionIdAndExerciseId(sessionId, exerciseId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.removeSessionExercise(sessionId, exerciseId, userId))
                    .isInstanceOf(SessionNotFoundException.class)
                    .hasMessageContaining("Ejercicio");
        }

        @Test
        @DisplayName("Sesión de otro usuario lanza SessionNotFoundException")
        void foreignSession_throws() {
            WorkoutSession session = buildSession();
            session.setUserId(UUID.randomUUID());
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

            assertThatThrownBy(() -> service.removeSessionExercise(sessionId, exerciseId, userId))
                    .isInstanceOf(SessionNotFoundException.class);
            verify(sessionExerciseRepository, never()).findBySessionIdAndExerciseId(any(), any());
        }
    }
}
