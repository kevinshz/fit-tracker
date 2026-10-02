package com.fittracker.workout.presentation.controller;

import com.fittracker.workout.presentation.dto.RoutineRequest;
import com.fittracker.workout.presentation.dto.RoutineResponse;
import com.fittracker.workout.service.interfaces.RoutineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workouts/routines")
@RequiredArgsConstructor
@Tag(name = "Routines", description = "Rutinas de entrenamiento personalizadas")
public class RoutineController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final RoutineService routineService;

    @Operation(summary = "Crear una rutina",
            description = "Crea una rutina con sus ejercicios planificados para el usuario autenticado (X-User-Id). Devuelve 201.")
    @PostMapping
    public ResponseEntity<RoutineResponse> createRoutine(
            @Valid @RequestBody RoutineRequest request,
            @RequestHeader(USER_ID_HEADER) UUID userId) {
        RoutineResponse response = routineService.createRoutine(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Listar las rutinas del usuario",
            description = "Devuelve las rutinas ordenadas de mas reciente a mas antigua.")
    @GetMapping
    public ResponseEntity<List<RoutineResponse>> listRoutines(
            @RequestHeader(USER_ID_HEADER) UUID userId) {
        return ResponseEntity.ok(routineService.listRoutines(userId));
    }

    @Operation(summary = "Obtener una rutina por id",
            description = "Incluye los ejercicios planificados en orden. Devuelve 404 si no existe o no pertenece al usuario.")
    @GetMapping("/{id}")
    public ResponseEntity<RoutineResponse> getRoutine(
            @PathVariable UUID id,
            @RequestHeader(USER_ID_HEADER) UUID userId) {
        return ResponseEntity.ok(routineService.getRoutine(id, userId));
    }

    @Operation(summary = "Actualizar una rutina",
            description = "Reemplaza nombre, metadatos y ejercicios planificados. Devuelve 200.")
    @PutMapping("/{id}")
    public ResponseEntity<RoutineResponse> updateRoutine(
            @PathVariable UUID id,
            @Valid @RequestBody RoutineRequest request,
            @RequestHeader(USER_ID_HEADER) UUID userId) {
        return ResponseEntity.ok(routineService.updateRoutine(id, request, userId));
    }

    @Operation(summary = "Eliminar una rutina", description = "Devuelve 204 sin contenido.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoutine(
            @PathVariable UUID id,
            @RequestHeader(USER_ID_HEADER) UUID userId) {
        routineService.deleteRoutine(id, userId);
        return ResponseEntity.noContent().build();
    }
}
