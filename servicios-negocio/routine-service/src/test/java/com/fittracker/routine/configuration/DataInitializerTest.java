package com.fittracker.routine.configuration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fittracker.routine.persistence.entity.Exercise;
import com.fittracker.routine.persistence.entity.ExerciseAlias;
import com.fittracker.routine.persistence.entity.enums.Equipment;
import com.fittracker.routine.persistence.entity.enums.ExerciseType;
import com.fittracker.routine.persistence.entity.enums.MuscleGroup;
import com.fittracker.routine.persistence.entity.enums.ProgressionStrategy;
import com.fittracker.routine.persistence.repository.ExerciseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DataInitializer - Seeding y traduccion del catalogo")
class DataInitializerTest {

    @Mock private ExerciseRepository exerciseRepository;
    @Spy private ObjectMapper objectMapper = new ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @InjectMocks private DataInitializer dataInitializer;

    private Exercise buildExercise(String externalId, String name, List<String> aliases) {
        var exercise = Exercise.builder()
                .id(UUID.randomUUID())
                .externalId(externalId)
                .name(name)
                .type(ExerciseType.STRENGTH)
                .primaryMuscle(MuscleGroup.CHEST)
                .secondaryMuscles(new ArrayList<>())
                .equipment(Equipment.BARBELL)
                .progressionStrategy(ProgressionStrategy.DOUBLE_PROGRESSION)
                .isCustom(false)
                .aliases(new ArrayList<>())
                .build();
        for (String alias : aliases) {
            exercise.getAliases().add(ExerciseAlias.builder().alias(alias).exercise(exercise).build());
        }
        return exercise;
    }

    private Map<String, String> readSpanishNames() throws Exception {
        try (InputStream is = new ClassPathResource("data/exercise-names-es.json").getInputStream()) {
            return objectMapper.readValue(is, new TypeReference<Map<String, String>>() {
            });
        }
    }

    @Test
    @DisplayName("exercise-names-es.json cubre exactamente los ejercicios sembrados (1318) con nombres unicos")
    void exerciseNamesEs_cubreElCatalogoSembrado() throws Exception {
        Map<String, String> nombresEs = readSpanishNames();
        List<Map<String, Object>> catalogo;
        try (InputStream is = new ClassPathResource("data/exercises.json").getInputStream()) {
            catalogo = objectMapper.readValue(is, new TypeReference<List<Map<String, Object>>>() {
            });
        }

        Set<String> nombresVistos = new HashSet<>();
        Set<String> externalIdsSembrados = new LinkedHashSet<>();
        for (var dto : catalogo) {
            Object nombre = dto.get("name");
            if (nombre == null || nombre.toString().isBlank()) {
                continue;
            }
            if (!nombresVistos.add(nombre.toString().trim())) {
                continue;
            }
            externalIdsSembrados.add(String.valueOf(dto.get("id")));
        }

        assertThat(externalIdsSembrados).hasSize(1318);
        assertThat(nombresEs.keySet()).containsExactlyInAnyOrderElementsOf(externalIdsSembrados);
        assertThat(nombresEs.values()).doesNotHaveDuplicates();
        assertThat(nombresEs.values()).allMatch(valor -> valor != null && !valor.isBlank() && valor.length() <= 200);
    }

    @Test
    @DisplayName("Seeding inserta exactamente los externalId con traduccion disponible")
    void seedCatalog_insertaElCatalogoCompleto() throws Exception {
        when(exerciseRepository.count()).thenReturn(0L);
        List<Exercise> guardados = new ArrayList<>();
        when(exerciseRepository.saveAll(any())).thenAnswer(invocation -> {
            List<Exercise> lote = invocation.getArgument(0);
            guardados.addAll(lote);
            return lote;
        });

        dataInitializer.run(new DefaultApplicationArguments());

        assertThat(guardados).extracting(Exercise::getExternalId)
                .containsExactlyInAnyOrderElementsOf(readSpanishNames().keySet());
    }

    @Test
    @DisplayName("Traduccion renombra a espanol conservando el alias en ingles y es idempotente")
    void translateCatalog_renombraYConservaAliasIngles() throws Exception {
        var pushUp = buildExercise("0662", "push-up", List.of("push-up"));
        when(exerciseRepository.count()).thenReturn(1L);
        when(exerciseRepository.findAll()).thenReturn(List.of(pushUp));
        when(exerciseRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        dataInitializer.run(new DefaultApplicationArguments());

        assertThat(pushUp.getName()).isEqualTo("Flexión");
        assertThat(pushUp.getAliases()).extracting(ExerciseAlias::getAlias).containsExactly("push-up");
        verify(exerciseRepository, times(1)).saveAll(any());

        dataInitializer.run(new DefaultApplicationArguments());

        assertThat(pushUp.getName()).isEqualTo("Flexión");
        verify(exerciseRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("Traduccion omite el renombre si el nombre en espanol ya existe (uk_exercise_name)")
    void translateCatalog_omiteSiColisionaConNombreExistente() throws Exception {
        var pullUp = buildExercise("0652", "pull-up", List.of("pull-up"));
        var dominada = buildExercise(null, "Dominada", List.of());
        when(exerciseRepository.count()).thenReturn(1L);
        when(exerciseRepository.findAll()).thenReturn(List.of(pullUp, dominada));

        dataInitializer.run(new DefaultApplicationArguments());

        assertThat(pullUp.getName()).isEqualTo("pull-up");
        verify(exerciseRepository, never()).saveAll(any());
    }
}
