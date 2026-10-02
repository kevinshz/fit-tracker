package com.fittracker.routine.service.implementation;

import com.fittracker.routine.persistence.entity.Exercise;
import com.fittracker.routine.persistence.entity.ExerciseAlias;
import com.fittracker.routine.persistence.entity.enums.Equipment;
import com.fittracker.routine.persistence.entity.enums.MuscleGroup;
import com.fittracker.routine.persistence.repository.ExerciseRepository;
import com.fittracker.routine.presentation.dto.ExerciseRequest;
import com.fittracker.routine.presentation.dto.ExerciseResponse;
import com.fittracker.routine.presentation.dto.ExerciseSummaryDto;
import com.fittracker.routine.service.interfaces.ExerciseService;
import com.fittracker.routine.utils.mapper.ExerciseMapper;
import com.fittracker.common.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExerciseServiceImpl implements ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final ExerciseMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ExerciseSummaryDto> findAll(String muscle, String equipment, Pageable pageable) {
        Specification<Exercise> spec = Specification.where(null);

        if (muscle != null && !muscle.isBlank()) {
            var value = MuscleGroup.valueOf(muscle.trim().toUpperCase());
            spec = spec.and((root, query, cb) -> cb.equal(root.get("primaryMuscle"), value));
        }
        if (equipment != null && !equipment.isBlank()) {
            var value = Equipment.valueOf(equipment.trim().toUpperCase());
            spec = spec.and((root, query, cb) -> cb.equal(root.get("equipment"), value));
        }

        return exerciseRepository.findAll(spec, pageable).map(mapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExerciseSummaryDto> search(String query, Pageable pageable) {
        return exerciseRepository.searchByNameOrAlias(query.toLowerCase(), pageable).map(mapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public ExerciseResponse findById(UUID id) {
        var exercise = getExerciseOrThrow(id);
        return mapper.toResponse(exercise);
    }

    @Override
    @Transactional
    public ExerciseResponse create(ExerciseRequest request, String createdBy) {
        validateUniqueNameAndAliases(request.name(), request.aliases(), null);

        var exercise = Exercise.builder()
                .name(request.name())
                .type(request.type())
                .primaryMuscle(request.primaryMuscle())
                .secondaryMuscles(request.secondaryMuscles() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(request.secondaryMuscles()))
                .equipment(request.equipment())
                .progressionStrategy(request.progressionStrategy() == null
                        ? mapper.defaultProgression()
                        : request.progressionStrategy())
                .isCustom(true)
                .createdBy(createdBy)
                .build();

        addAliases(exercise, request.aliases());
        return mapper.toResponse(exerciseRepository.save(exercise));
    }

    @Override
    @Transactional
    public ExerciseResponse update(UUID id, ExerciseRequest request) {
        var exercise = getExerciseOrThrow(id);
        assertCustom(exercise);

        validateUniqueNameAndAliases(request.name(), request.aliases(), id);

        mapper.updateEntity(exercise, request);
        replaceAliases(exercise, request.aliases());
        return mapper.toResponse(exerciseRepository.save(exercise));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        var exercise = getExerciseOrThrow(id);
        assertCustom(exercise);
        exerciseRepository.delete(exercise);
    }

    private Exercise getExerciseOrThrow(UUID id) {
        return exerciseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("no existe un ejercicio con id " + id));
    }

    private void assertCustom(Exercise exercise) {
        if (!exercise.isCustom()) {
            throw new IllegalStateException("el catálogo base no puede modificarse; solo los ejercicios custom");
        }
    }

    private void validateUniqueNameAndAliases(String name, List<String> aliases, UUID excludeId) {
        var byName = exerciseRepository.findByNameIgnoreCase(name);
        if (byName.isPresent() && (excludeId == null || !byName.get().getId().equals(excludeId))) {
            throw new IllegalArgumentException("ya existe un ejercicio llamado '" + name + "'");
        }
        if (aliases == null) {
            return;
        }
        for (var alias : aliases) {
            if (alias.equalsIgnoreCase(name)) {
                continue;
            }
            var existing = exerciseRepository.findByNameIgnoreCase(alias);
            if (existing.isPresent() && (excludeId == null || !existing.get().getId().equals(excludeId))) {
                throw new IllegalArgumentException("'" + alias + "' ya se usa como nombre de otro ejercicio");
            }
        }
    }

    private void addAliases(Exercise exercise, List<String> aliases) {
        if (aliases == null) {
            return;
        }
        for (var aliasValue : aliases) {
            var alias = ExerciseAlias.builder()
                    .alias(aliasValue)
                    .exercise(exercise)
                    .build();
            exercise.getAliases().add(alias);
        }
    }

    private void replaceAliases(Exercise exercise, List<String> aliases) {
        exercise.getAliases().clear();
        addAliases(exercise, aliases);
    }
}
