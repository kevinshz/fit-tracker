package com.fittracker.routine.service.implementation;

import com.fittracker.common.exceptions.ResourceNotFoundException;
import com.fittracker.routine.persistence.entity.Exercise;
import com.fittracker.routine.persistence.entity.enums.Equipment;
import com.fittracker.routine.persistence.entity.enums.ExerciseType;
import com.fittracker.routine.persistence.entity.enums.MuscleGroup;
import com.fittracker.routine.persistence.entity.enums.ProgressionStrategy;
import com.fittracker.routine.persistence.repository.ExerciseRepository;
import com.fittracker.routine.presentation.dto.ExerciseRequest;
import com.fittracker.routine.utils.mapper.ExerciseMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExerciseServiceImpl - Tests Unitarios")
class ExerciseServiceImplTest {

    @Mock private ExerciseRepository exerciseRepository;
    @Spy private ExerciseMapper mapper = new ExerciseMapper();

    @InjectMocks private ExerciseServiceImpl service;

    private Exercise buildExercise(boolean custom) {
        return Exercise.builder()
                .id(UUID.randomUUID())
                .name("Press banca")
                .type(ExerciseType.STRENGTH)
                .primaryMuscle(MuscleGroup.CHEST)
                .secondaryMuscles(new java.util.ArrayList<>())
                .equipment(Equipment.BARBELL)
                .progressionStrategy(ProgressionStrategy.DOUBLE_PROGRESSION)
                .isCustom(custom)
                .createdBy(custom ? "user-1" : null)
                .aliases(new java.util.ArrayList<>())
                .build();
    }

    private ExerciseRequest buildRequest(String name) {
        return new ExerciseRequest(name, ExerciseType.STRENGTH, MuscleGroup.CHEST,
                null, Equipment.BARBELL, null, null);
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("Retorna el ejercicio encontrado")
        void found() {
            Exercise exercise = buildExercise(true);
            when(exerciseRepository.findById(exercise.getId())).thenReturn(Optional.of(exercise));

            var response = service.findById(exercise.getId());

            assertThat(response.name()).isEqualTo("Press banca");
        }

        @Test
        @DisplayName("No encontrado lanza ResourceNotFoundException")
        void notFound_throws() {
            UUID id = UUID.randomUUID();
            when(exerciseRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("Happy path: crea ejercicio custom con alias y progression default")
        void success() {
            when(exerciseRepository.findByNameIgnoreCase("Press banca")).thenReturn(Optional.empty());
            when(exerciseRepository.save(any(Exercise.class))).thenAnswer(inv -> {
                Exercise e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return e;
            });
            var request = new ExerciseRequest("Press banca", ExerciseType.STRENGTH, MuscleGroup.CHEST,
                    List.of(MuscleGroup.TRICEPS), Equipment.BARBELL, null, List.of("Bench"));

            var response = service.create(request, "user-1");

            assertThat(response.isCustom()).isTrue();
            assertThat(response.createdBy()).isEqualTo("user-1");
            assertThat(response.progressionStrategy()).isEqualTo(ProgressionStrategy.DOUBLE_PROGRESSION);
            assertThat(response.aliases()).containsExactly("Bench");
        }

        @Test
        @DisplayName("Nombre duplicado lanza IllegalArgumentException")
        void duplicateName_throws() {
            when(exerciseRepository.findByNameIgnoreCase("Press banca"))
                    .thenReturn(Optional.of(buildExercise(false)));
            var request = buildRequest("Press banca");

            assertThatThrownBy(() -> service.create(request, "user-1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ya existe");
            verify(exerciseRepository, never()).save(any());
        }

        @Test
        @DisplayName("Alias duplicado con otro ejercicio lanza IllegalArgumentException")
        void duplicateAlias_throws() {
            when(exerciseRepository.findByNameIgnoreCase("Press plano")).thenReturn(Optional.empty());
            when(exerciseRepository.findByNameIgnoreCase("Bench"))
                    .thenReturn(Optional.of(buildExercise(false)));
            var request = new ExerciseRequest("Press plano", ExerciseType.STRENGTH, MuscleGroup.CHEST,
                    null, Equipment.BARBELL, null, List.of("Bench"));

            assertThatThrownBy(() -> service.create(request, "user-1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ya se usa como nombre");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("Ejercicio custom se actualiza")
        void customExercise_updated() {
            Exercise custom = buildExercise(true);
            when(exerciseRepository.findById(custom.getId())).thenReturn(Optional.of(custom));
            when(exerciseRepository.findByNameIgnoreCase("Press inclinado")).thenReturn(Optional.empty());
            when(exerciseRepository.save(any(Exercise.class))).thenAnswer(inv -> inv.getArgument(0));
            var request = buildRequest("Press inclinado");

            var response = service.update(custom.getId(), request);

            assertThat(response.name()).isEqualTo("Press inclinado");
        }

        @Test
        @DisplayName("Ejercicio del catálogo base no se puede modificar")
        void baseCatalog_throws() {
            Exercise base = buildExercise(false);
            when(exerciseRepository.findById(base.getId())).thenReturn(Optional.of(base));

            assertThatThrownBy(() -> service.update(base.getId(), buildRequest("Otro nombre")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("catálogo base");
            verify(exerciseRepository, never()).save(any());
        }

        @Test
        @DisplayName("Ejercicio no encontrado lanza ResourceNotFoundException")
        void notFound_throws() {
            UUID id = UUID.randomUUID();
            when(exerciseRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(id, buildRequest("x")))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("Ejercicio custom se elimina")
        void customExercise_deleted() {
            Exercise custom = buildExercise(true);
            when(exerciseRepository.findById(custom.getId())).thenReturn(Optional.of(custom));

            service.delete(custom.getId());

            verify(exerciseRepository).delete(custom);
        }

        @Test
        @DisplayName("Ejercicio del catálogo base no se puede eliminar")
        void baseCatalog_throws() {
            Exercise base = buildExercise(false);
            when(exerciseRepository.findById(base.getId())).thenReturn(Optional.of(base));

            assertThatThrownBy(() -> service.delete(base.getId()))
                    .isInstanceOf(IllegalStateException.class);
            verify(exerciseRepository, never()).delete(any(Exercise.class));
        }
    }
}
