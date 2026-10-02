package com.fittracker.routine.persistence.repository;

import com.fittracker.routine.AbstractPostgresIntegrationTest;
import com.fittracker.routine.persistence.entity.Exercise;
import com.fittracker.routine.persistence.entity.ExerciseAlias;
import com.fittracker.routine.persistence.entity.enums.Equipment;
import com.fittracker.routine.persistence.entity.enums.ExerciseType;
import com.fittracker.routine.persistence.entity.enums.MuscleGroup;
import com.fittracker.routine.persistence.entity.enums.ProgressionStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@EnabledIfSystemProperty(named = "testcontainers.enabled", matches = "true",
        disabledReason = "Requiere Docker (usar -Dtestcontainers.enabled=true)")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("ExerciseRepository - Integracion con PostgreSQL (Testcontainers)")
class ExerciseRepositoryTest extends AbstractPostgresIntegrationTest {

    @Autowired private ExerciseRepository repository;

    private Exercise buildExercise(String name) {
        return Exercise.builder()
                .name(name)
                .type(ExerciseType.STRENGTH)
                .primaryMuscle(MuscleGroup.CHEST)
                .secondaryMuscles(new ArrayList<>())
                .equipment(Equipment.BARBELL)
                .progressionStrategy(ProgressionStrategy.DOUBLE_PROGRESSION)
                .isCustom(true)
                .createdBy("test")
                .aliases(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Guardar y buscar ejercicio por nombre (case-insensitive)")
    void saveAndFindByNameIgnoreCase() {
        repository.save(buildExercise("Press Banca"));

        Optional<Exercise> found = repository.findByNameIgnoreCase("press banca");

        assertThat(found).isPresent();
        assertThat(found.get().getPrimaryMuscle()).isEqualTo(MuscleGroup.CHEST);
    }

    @Test
    @DisplayName("Nombre unico: viola restriccion al duplicar")
    void duplicateName_violatesUniqueConstraint() {
        repository.save(buildExercise("Sentadilla"));

        assertThatThrownBy(() -> {
            repository.save(buildExercise("Sentadilla"));
            repository.flush();
        }).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("existsByNameIgnoreCase")
    void existsByNameIgnoreCase() {
        repository.save(buildExercise("Dominadas"));

        assertThat(repository.existsByNameIgnoreCase("dominadas")).isTrue();
        assertThat(repository.existsByNameIgnoreCase("no-existe")).isFalse();
    }

    @Test
    @DisplayName("Busqueda por nombre o alias con JPQL (alias incluido)")
    void searchByNameOrAlias() {
        Exercise bench = buildExercise("Press Banca");
        bench.getAliases().add(ExerciseAlias.builder().alias("Bench Press").exercise(bench).build());
        repository.save(bench);
        repository.save(buildExercise("Sentadilla"));

        Page<Exercise> byName = repository.searchByNameOrAlias("press", PageRequest.of(0, 10));
        Page<Exercise> byAlias = repository.searchByNameOrAlias("bench", PageRequest.of(0, 10));

        assertThat(byName.getContent()).extracting(Exercise::getName).containsExactly("Press Banca");
        assertThat(byAlias.getContent()).extracting(Exercise::getName).containsExactly("Press Banca");
    }

    @Test
    @DisplayName("Busqueda que no coincide retorna vacio")
    void search_noMatch_returnsEmpty() {
        repository.save(buildExercise("Press Banca"));

        Page<Exercise> result = repository.searchByNameOrAlias("zzz", PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Filtro Specification por musculo primario")
    void specification_filterByMuscle() {
        Exercise chest = buildExercise("Press Banca");
        Exercise legs = buildExercise("Sentadilla");
        legs.setPrimaryMuscle(MuscleGroup.QUADRICEPS);
        repository.save(chest);
        repository.save(legs);

        Page<Exercise> result = repository.findAll(
                Specification.<Exercise>where((root, query, cb) ->
                        cb.equal(root.get("primaryMuscle"), MuscleGroup.QUADRICEPS)),
                PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Exercise::getName).containsExactly("Sentadilla");
    }

    @Test
    @DisplayName("Borrado de ejercicio custom")
    void deleteExercise() {
        Exercise exercise = repository.save(buildExercise("Elevaciones laterales"));
        UUID id = exercise.getId();

        repository.deleteById(id);

        assertThat(repository.findById(id)).isEmpty();
    }
}
