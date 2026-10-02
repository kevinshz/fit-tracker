package com.fittracker.routine.presentation.controller;

import com.fittracker.routine.persistence.entity.enums.Equipment;
import com.fittracker.routine.persistence.entity.enums.ExerciseType;
import com.fittracker.routine.persistence.entity.enums.MuscleGroup;
import com.fittracker.routine.persistence.entity.enums.ProgressionStrategy;
import com.fittracker.routine.presentation.dto.ExerciseRequest;
import com.fittracker.routine.presentation.dto.ExerciseResponse;
import com.fittracker.routine.presentation.dto.ExerciseSummaryDto;
import com.fittracker.routine.service.interfaces.ExerciseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exercises")
@RequiredArgsConstructor
@Tag(name = "Exercises", description = "Catalogo de ejercicios: CRUD, busqueda y metadatos")
public class ExerciseController {

    private final ExerciseService exerciseService;

    @Operation(summary = "Listar ejercicios paginados",
            description = "Filtra opcionalmente por musculo principal y equipamiento. Ordenado por nombre.")
    @GetMapping
    public ResponseEntity<Page<ExerciseSummaryDto>> findAll(
            @RequestParam(required = false) String muscle,
            @RequestParam(required = false) String equipment,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by("name").ascending());
        return ResponseEntity.ok(exerciseService.findAll(muscle, equipment, pageable));
    }

    @Operation(summary = "Buscar ejercicios por texto",
            description = "Busca por nombre o alias (case-insensitive). Requiere el parametro q.")
    @GetMapping("/search")
    public ResponseEntity<Page<ExerciseSummaryDto>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by("name").ascending());
        return ResponseEntity.ok(exerciseService.search(q, pageable));
    }

    @Operation(summary = "Obtener un ejercicio por id")
    @GetMapping("/{id}")
    public ResponseEntity<ExerciseResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(exerciseService.findById(id));
    }

    @Operation(summary = "Crear un ejercicio personalizado",
            description = "Usa el header X-User-Id del gateway como creador. Devuelve 400 si el nombre ya existe.")
    @PostMapping
    public ResponseEntity<ExerciseResponse> create(
            @Valid @RequestBody ExerciseRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String createdBy
    ) {
        var created = exerciseService.create(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Actualizar un ejercicio")
    @PutMapping("/{id}")
    public ResponseEntity<ExerciseResponse> update(@PathVariable UUID id, @Valid @RequestBody ExerciseRequest request) {
        return ResponseEntity.ok(exerciseService.update(id, request));
    }

    @Operation(summary = "Eliminar un ejercicio",
            description = "Devuelve 204 sin contenido.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        exerciseService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Metadatos del catalogo",
            description = "Enumera musculos, tipos, equipamientos y estrategias de progresion disponibles.")
    @GetMapping("/metadata")
    public ResponseEntity<Map<String, List<String>>> metadata() {
        return ResponseEntity.ok(Map.of(
                "muscles", enumNames(MuscleGroup.values()),
                "types", enumNames(ExerciseType.values()),
                "equipment", enumNames(Equipment.values()),
                "progressionStrategies", enumNames(ProgressionStrategy.values())
        ));
    }

    private List<String> enumNames(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toList();
    }
}
