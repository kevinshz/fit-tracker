package com.fittracker.workout.service.implementation;

import com.fittracker.workout.persistence.entity.WorkoutRoutine;
import com.fittracker.workout.persistence.entity.enums.MuscleLabel;
import com.fittracker.workout.persistence.entity.enums.SessionType;
import com.fittracker.workout.persistence.repository.WorkoutRoutineRepository;
import com.fittracker.workout.presentation.dto.RoutineExerciseRequest;
import com.fittracker.workout.presentation.dto.RoutineRequest;
import com.fittracker.workout.service.exception.InvalidRoutineException;
import com.fittracker.workout.service.exception.RoutineNotFoundException;
import com.fittracker.workout.service.http.RoutineClient;
import com.fittracker.workout.service.http.dto.ExerciseSummaryDto;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoutineServiceImpl - Tests Unitarios")
class RoutineServiceImplTest {

    @Mock private WorkoutRoutineRepository routineRepository;
    @Spy private WorkoutMapper mapper = new WorkoutMapper();
    @Mock private RoutineClient routineClient;
    @InjectMocks private RoutineServiceImpl service;

    private final UUID userId = UUID.randomUUID();
    private final UUID routineId = UUID.randomUUID();
    private final UUID exerciseId = UUID.randomUUID();

    private RoutineRequest buildRequest(Integer plannedSets) {
        return new RoutineRequest(
                "Día de Pecho",
                SessionType.STRENGTH,
                MuscleLabel.CHEST_BACK,
                "Press y aperturas",
                List.of(new RoutineExerciseRequest(exerciseId, plannedSets)));
    }

    private WorkoutRoutine buildRoutine() {
        return WorkoutRoutine.builder()
                .id(routineId)
                .userId(userId)
                .name("Día de Pecho")
                .sessionType(SessionType.STRENGTH)
                .muscleLabel(MuscleLabel.CHEST_BACK)
                .exercises(new ArrayList<>())
                .build();
    }

    private void mockCatalog() {
        when(routineClient.getExercise(exerciseId))
                .thenReturn(new ExerciseSummaryDto(exerciseId, "Press banca", "CHEST", "BARBELL"));
    }

    @Nested
    @DisplayName("createRoutine")
    class CreateRoutine {

        @Test
        @DisplayName("Happy path: 201 con nombre resuelto del catálogo y 3 series por defecto")
        void happyPath_createsRoutine() {
            mockCatalog();
            when(routineRepository.save(any(WorkoutRoutine.class))).thenAnswer(inv -> {
                WorkoutRoutine r = inv.getArgument(0);
                r.setId(routineId);
                return r;
            });

            var response = service.createRoutine(buildRequest(null), userId);

            assertThat(response.id()).isEqualTo(routineId);
            assertThat(response.name()).isEqualTo("Día de Pecho");
            assertThat(response.exercises()).hasSize(1);
            assertThat(response.exercises().get(0).exerciseName()).isEqualTo("Press banca");
            assertThat(response.exercises().get(0).plannedSets()).isEqualTo(3);
            assertThat(response.exercises().get(0).position()).isEqualTo(1);
        }

        @Test
        @DisplayName("plannedSets explicito se respeta")
        void explicitPlannedSets() {
            mockCatalog();
            when(routineRepository.save(any(WorkoutRoutine.class))).thenAnswer(inv -> {
                WorkoutRoutine r = inv.getArgument(0);
                r.setId(routineId);
                return r;
            });

            var response = service.createRoutine(buildRequest(5), userId);

            assertThat(response.exercises().get(0).plannedSets()).isEqualTo(5);
        }

        @Test
        @DisplayName("Nombre en blanco lanza InvalidRoutineException")
        void blankName_throws() {
            var request = new RoutineRequest("   ", SessionType.STRENGTH,
                    MuscleLabel.CHEST_BACK, null,
                    List.of(new RoutineExerciseRequest(exerciseId, null)));

            assertThatThrownBy(() -> service.createRoutine(request, userId))
                    .isInstanceOf(InvalidRoutineException.class)
                    .hasMessageContaining("nombre");
            verify(routineRepository, never()).save(any());
        }

        @Test
        @DisplayName("Sin ejercicios lanza InvalidRoutineException")
        void emptyExercises_throws() {
            var request = new RoutineRequest("Día de Pecho", SessionType.STRENGTH,
                    MuscleLabel.CHEST_BACK, null, List.of());

            assertThatThrownBy(() -> service.createRoutine(request, userId))
                    .isInstanceOf(InvalidRoutineException.class)
                    .hasMessageContaining("ejercicio");
            verify(routineRepository, never()).save(any());
        }

        @Test
        @DisplayName("plannedSets < 1 lanza InvalidRoutineException")
        void invalidPlannedSets_throws() {
            var request = buildRequest(0);

            assertThatThrownBy(() -> service.createRoutine(request, userId))
                    .isInstanceOf(InvalidRoutineException.class)
                    .hasMessageContaining("series");
            verify(routineRepository, never()).save(any());
        }

        @Test
        @DisplayName("Ejercicio inexistente en el catálogo lanza InvalidRoutineException")
        void unknownExercise_throws() {
            when(routineClient.getExercise(exerciseId)).thenThrow(new RuntimeException("404"));

            assertThatThrownBy(() -> service.createRoutine(buildRequest(null), userId))
                    .isInstanceOf(InvalidRoutineException.class);
            verify(routineRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("listRoutines")
    class ListRoutines {

        @Test
        @DisplayName("Lista las rutinas del usuario")
        void listsUserRoutines() {
            WorkoutRoutine routine = buildRoutine();
            routine.getExercises().add(com.fittracker.workout.persistence.entity.WorkoutRoutineExercise.builder()
                    .routine(routine).exerciseId(exerciseId).exerciseName("Press banca")
                    .position(1).plannedSets(3).build());
            when(routineRepository.findByUserIdOrderByCreatedAtDesc(userId))
                    .thenReturn(List.of(routine));

            var result = service.listRoutines(userId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).exercises()).hasSize(1);
            verify(routineRepository).findByUserIdOrderByCreatedAtDesc(userId);
        }

        @Test
        @DisplayName("Sin rutinas devuelve lista vacía")
        void empty_returnsEmpty() {
            when(routineRepository.findByUserIdOrderByCreatedAtDesc(userId))
                    .thenReturn(List.of());

            assertThat(service.listRoutines(userId)).isEmpty();
        }
    }

    @Nested
    @DisplayName("getRoutine")
    class GetRoutine {

        @Test
        @DisplayName("Rutina propia se retorna")
        void ownRoutine_returns() {
            when(routineRepository.findById(routineId)).thenReturn(Optional.of(buildRoutine()));

            var response = service.getRoutine(routineId, userId);

            assertThat(response.id()).isEqualTo(routineId);
        }

        @Test
        @DisplayName("Rutina de otro usuario lanza RoutineNotFoundException")
        void foreignRoutine_throws() {
            WorkoutRoutine routine = buildRoutine();
            routine.setUserId(UUID.randomUUID());
            when(routineRepository.findById(routineId)).thenReturn(Optional.of(routine));

            assertThatThrownBy(() -> service.getRoutine(routineId, userId))
                    .isInstanceOf(RoutineNotFoundException.class);
        }

        @Test
        @DisplayName("Rutina inexistente lanza RoutineNotFoundException")
        void notFound_throws() {
            when(routineRepository.findById(routineId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getRoutine(routineId, userId))
                    .isInstanceOf(RoutineNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateRoutine")
    class UpdateRoutine {

        @Test
        @DisplayName("Reemplaza metadatos y ejercicios, recalculando posiciones")
        void updatesFieldsAndExercises() {
            WorkoutRoutine routine = buildRoutine();
            routine.getExercises().add(com.fittracker.workout.persistence.entity.WorkoutRoutineExercise.builder()
                    .routine(routine).exerciseId(UUID.randomUUID()).exerciseName("Ejercicio viejo")
                    .position(1).plannedSets(2).build());
            when(routineRepository.findById(routineId)).thenReturn(Optional.of(routine));
            mockCatalog();
            when(routineRepository.save(any(WorkoutRoutine.class))).thenAnswer(inv -> inv.getArgument(0));

            var request = new RoutineRequest("Pierna Pesado", SessionType.HYPERTROPHY,
                    MuscleLabel.LEGS, null,
                    List.of(new RoutineExerciseRequest(exerciseId, 4)));
            var response = service.updateRoutine(routineId, request, userId);

            assertThat(response.name()).isEqualTo("Pierna Pesado");
            assertThat(response.sessionType()).isEqualTo(SessionType.HYPERTROPHY);
            assertThat(response.exercises()).hasSize(1);
            assertThat(response.exercises().get(0).exerciseName()).isEqualTo("Press banca");
            assertThat(response.exercises().get(0).plannedSets()).isEqualTo(4);
        }

        @Test
        @DisplayName("Rutina de otro usuario lanza RoutineNotFoundException")
        void foreignRoutine_throws() {
            WorkoutRoutine routine = buildRoutine();
            routine.setUserId(UUID.randomUUID());
            when(routineRepository.findById(routineId)).thenReturn(Optional.of(routine));

            assertThatThrownBy(() -> service.updateRoutine(routineId, buildRequest(null), userId))
                    .isInstanceOf(RoutineNotFoundException.class);
            verify(routineRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteRoutine")
    class DeleteRoutine {

        @Test
        @DisplayName("Rutina propia se elimina")
        void ownRoutine_deletes() {
            WorkoutRoutine routine = buildRoutine();
            when(routineRepository.findById(routineId)).thenReturn(Optional.of(routine));

            service.deleteRoutine(routineId, userId);

            verify(routineRepository).delete(routine);
        }

        @Test
        @DisplayName("Rutina de otro usuario lanza RoutineNotFoundException y no elimina")
        void foreignRoutine_throws() {
            WorkoutRoutine routine = buildRoutine();
            routine.setUserId(UUID.randomUUID());
            when(routineRepository.findById(routineId)).thenReturn(Optional.of(routine));

            assertThatThrownBy(() -> service.deleteRoutine(routineId, userId))
                    .isInstanceOf(RoutineNotFoundException.class);
            verify(routineRepository, never()).delete(any());
        }
    }
}
