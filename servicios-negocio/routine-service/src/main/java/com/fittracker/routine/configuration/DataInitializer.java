package com.fittracker.routine.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fittracker.routine.persistence.entity.Exercise;
import com.fittracker.routine.persistence.entity.ExerciseAlias;
import com.fittracker.routine.persistence.entity.enums.Equipment;
import com.fittracker.routine.persistence.entity.enums.ExerciseType;
import com.fittracker.routine.persistence.entity.enums.MuscleGroup;
import com.fittracker.routine.persistence.entity.enums.ProgressionStrategy;
import com.fittracker.routine.persistence.repository.ExerciseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final int BATCH_SIZE = 200;

    private final ExerciseRepository exerciseRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (exerciseRepository.count() > 0) {
            log.info("Catálogo de ejercicios ya inicializado; se omite el seeding");
        } else {
            seedCatalog();
        }
        translateCatalog();
    }

    /**
     * Paso idempotente: renombra los ejercicios a su nombre en español usando
     * exercise-names-es.json (clave = externalId). El nombre original en inglés
     * se conserva como alias para que la búsqueda siga funcionando en ambos idiomas.
     */
    private void translateCatalog() throws Exception {
        var resource = new ClassPathResource("data/exercise-names-es.json");
        if (!resource.exists()) {
            log.warn("exercise-names-es.json no encontrado; se omite la traducción del catálogo");
            return;
        }
        Map<String, String> nombresEs;
        try (InputStream is = resource.getInputStream()) {
            nombresEs = objectMapper.readValue(is, new TypeReference<Map<String, String>>() {
            });
        }
        if (nombresEs.isEmpty()) {
            return;
        }

        Map<String, Exercise> porExternalId = new HashMap<>();
        Set<String> nombresActuales = new HashSet<>();
        for (var ejercicio : exerciseRepository.findAll()) {
            if (ejercicio.getExternalId() != null) {
                porExternalId.put(ejercicio.getExternalId(), ejercicio);
            }
            nombresActuales.add(ejercicio.getName());
        }

        long traducidos = 0;
        long omitidos = 0;
        List<Exercise> lote = new ArrayList<>(BATCH_SIZE);
        for (var entry : nombresEs.entrySet()) {
            var ejercicio = porExternalId.get(entry.getKey());
            if (ejercicio == null) {
                omitidos++;
                continue;
            }
            String original = ejercicio.getName();
            String traducido = entry.getValue() == null ? null : entry.getValue().trim();
            if (traducido.isEmpty() || traducido.equals(original)) {
                continue;
            }
            if (traducido.length() > 200) {
                log.warn("Traducción para externalId {} excede 200 caracteres; se omite", entry.getKey());
                omitidos++;
                continue;
            }
            if (!nombresActuales.add(traducido)) {
                log.warn("Traducción '{}' (externalId {}) colisiona con un nombre existente; se omite",
                        traducido, entry.getKey());
                omitidos++;
                continue;
            }
            nombresActuales.remove(original);
            ejercicio.setName(traducido);
            asegurarAliasIngles(ejercicio, original);
            lote.add(ejercicio);
            traducidos++;
            if (lote.size() >= BATCH_SIZE) {
                exerciseRepository.saveAll(lote);
                lote.clear();
            }
        }
        if (!lote.isEmpty()) {
            exerciseRepository.saveAll(lote);
        }
        if (traducidos == 0) {
            log.info("Catálogo ya traducido al español; sin cambios");
        } else {
            log.info("Traducción completada: {} ejercicios renombrados, {} omitidos", traducidos, omitidos);
        }
    }

    private void asegurarAliasIngles(Exercise ejercicio, String nombreIngles) {
        boolean existe = ejercicio.getAliases().stream()
                .anyMatch(alias -> nombreIngles.equalsIgnoreCase(alias.getAlias()));
        if (!existe) {
            ejercicio.getAliases().add(ExerciseAlias.builder()
                    .alias(nombreIngles)
                    .exercise(ejercicio)
                    .build());
        }
    }

    private void seedCatalog() throws Exception {
        var resource = new ClassPathResource("data/exercises.json");
        long inserted = 0;
        long skipped = 0;
        List<Exercise> batch = new ArrayList<>(BATCH_SIZE);
        Set<String> nombresVistos = new HashSet<>();

        try (InputStream is = resource.getInputStream();
             JsonParser parser = objectMapper.getFactory().createParser(is)) {
            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new IllegalStateException("exercises.json no comienza con un array JSON");
            }
            while (parser.nextToken() == JsonToken.START_OBJECT) {
                ExerciseJsonDto dto = objectMapper.readValue(parser, ExerciseJsonDto.class);
                Optional<Exercise> mapped = mapToEntity(dto);
                if (mapped.isEmpty()) {
                    skipped++;
                    continue;
                }
                if (!nombresVistos.add(mapped.get().getName())) {
                    skipped++;
                    continue;
                }
                batch.add(mapped.get());
                if (batch.size() >= BATCH_SIZE) {
                    exerciseRepository.saveAll(batch);
                    inserted += batch.size();
                    batch.clear();
                }
            }
        }
        if (!batch.isEmpty()) {
            exerciseRepository.saveAll(batch);
            inserted += batch.size();
        }
        log.info("Seeding completado: {} ejercicios insertados, {} omitidos", inserted, skipped);
    }

    private Optional<Exercise> mapToEntity(ExerciseJsonDto dto) {
        if (dto.name() == null || dto.name().isBlank()) {
            return Optional.empty();
        }

        var equipment = mapEquipment(dto.equipment());
        var type = "cardio".equalsIgnoreCase(nullable(dto.bodyPart())) ? ExerciseType.CARDIO : ExerciseType.STRENGTH;

        var exercise = Exercise.builder()
                .externalId(dto.id())
                .name(dto.name().trim())
                .type(type)
                .primaryMuscle(mapMuscle(dto.target()))
                .secondaryMuscles(mapSecondaryMuscles(dto.secondaryMuscles()))
                .equipment(equipment)
                .progressionStrategy(ProgressionStrategy.DOUBLE_PROGRESSION)
                .isCustom(false)
                .createdBy(null)
                .instructionsEs(joinSteps(dto.instructionSteps() == null ? null : dto.instructionSteps().es()))
                .instructionsEn(joinSteps(dto.instructionSteps() == null ? null : dto.instructionSteps().en()))
                .imageUrl(dto.image())
                .gifUrl(dto.gifUrl())
                .attribution(dto.attribution())
                .build();

        exercise.getAliases().add(ExerciseAlias.builder()
                .alias(exercise.getName())
                .exercise(exercise)
                .build());

        return Optional.of(exercise);
    }

    private List<MuscleGroup> mapSecondaryMuscles(List<String> muscles) {
        if (muscles == null) {
            return new ArrayList<>();
        }
        Set<MuscleGroup> result = new HashSet<>();
        for (var muscle : muscles) {
            result.add(mapMuscle(muscle));
        }
        return new ArrayList<>(result);
    }

    private MuscleGroup mapMuscle(String value) {
        if (value == null || value.isBlank()) {
            return MuscleGroup.OTHER;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "abs" -> MuscleGroup.ABS;
            case "abductors" -> MuscleGroup.ABDUCTORS;
            case "adductors" -> MuscleGroup.ADDUCTORS;
            case "biceps" -> MuscleGroup.BICEPS;
            case "calves" -> MuscleGroup.CALVES;
            case "chest" -> MuscleGroup.CHEST;
            case "forearms" -> MuscleGroup.FOREARMS;
            case "glutes" -> MuscleGroup.GLUTES;
            case "hamstrings" -> MuscleGroup.HAMSTRINGS;
            case "lats", "upper back" -> MuscleGroup.LATS;
            case "lower back" -> MuscleGroup.LOWER_BACK;
            case "middle back" -> MuscleGroup.MIDDLE_BACK;
            case "neck" -> MuscleGroup.NECK;
            case "quadriceps", "quads", "upper legs", "legs" -> MuscleGroup.QUADRICEPS;
            case "shoulders" -> MuscleGroup.SHOULDERS;
            case "traps" -> MuscleGroup.TRAPS;
            case "triceps" -> MuscleGroup.TRICEPS;
            default -> MuscleGroup.OTHER;
        };
    }

    private Equipment mapEquipment(String value) {
        if (value == null || value.isBlank()) {
            return Equipment.OTHER;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "barbell" -> Equipment.BARBELL;
            case "dumbbell" -> Equipment.DUMBBELL;
            case "machine", "leverage machine", "smith machine" -> Equipment.MACHINE;
            case "cable", "cable machine" -> Equipment.CABLE;
            case "body weight", "bodyweight", "none" -> Equipment.BODY_WEIGHT;
            case "kettlebell" -> Equipment.KETTLEBELL;
            case "bands", "resistance band" -> Equipment.BANDS;
            case "medicine ball" -> Equipment.MEDICINE_BALL;
            case "exercise ball", "stability ball" -> Equipment.EXERCISE_BALL;
            case "e-z curl bar", "ez curl bar" -> Equipment.EZ_CURL_BAR;
            case "foam roll" -> Equipment.FOAM_ROLL;
            default -> Equipment.OTHER;
        };
    }

    private String joinSteps(List<String> steps) {
        if (steps == null || steps.isEmpty()) {
            return null;
        }
        return String.join("\n", steps);
    }

    private String nullable(String value) {
        return value == null ? "" : value;
    }

    record ExerciseJsonDto(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("body_part") String bodyPart,
            @JsonProperty("equipment") String equipment,
            @JsonProperty("target") String target,
            @JsonProperty("secondary_muscles") List<String> secondaryMuscles,
            @JsonProperty("instruction_steps") LanguageSteps instructionSteps,
            @JsonProperty("image") String image,
            @JsonProperty("gif_url") String gifUrl,
            @JsonProperty("attribution") String attribution
    ) {
    }

    record LanguageSteps(
            @JsonProperty("es") List<String> es,
            @JsonProperty("en") List<String> en
    ) {
    }
}
