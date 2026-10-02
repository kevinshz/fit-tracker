package com.fittracker.workout.presentation.controller;

import com.fittracker.workout.presentation.dto.AddSessionExerciseRequest;
import com.fittracker.workout.presentation.dto.LogSetRequest;
import com.fittracker.workout.presentation.dto.SessionExerciseDto;
import com.fittracker.workout.presentation.dto.SessionResponse;
import com.fittracker.workout.presentation.dto.SetResponse;
import com.fittracker.workout.presentation.dto.StartSessionRequest;
import com.fittracker.workout.service.interfaces.WorkoutSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workouts")
@RequiredArgsConstructor
@Tag(name = "Workouts", description = "Sesiones de entrenamiento y series")
public class WorkoutSessionController {

    private final WorkoutSessionService sessionService;

    @Operation(summary = "Iniciar una sesion de entrenamiento",
            description = "Crea la sesion para el usuario autenticado (X-User-Id). Si se envia routineId, copia los ejercicios planificados de la rutina. Devuelve 201.")
    @PostMapping("/sessions")
    public ResponseEntity<SessionResponse> startSession(
            @Valid @RequestBody StartSessionRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        SessionResponse response = sessionService.startSession(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Agregar un ejercicio a la sesion",
            description = "Resuelve el nombre del ejercicio contra el catalogo (routine-service). Si el ejercicio ya esta en la sesion, lo devuelve sin duplicar. Devuelve 201.")
    @PostMapping("/sessions/{id}/exercises")
    public ResponseEntity<SessionExerciseDto> addSessionExercise(
            @PathVariable UUID id,
            @Valid @RequestBody AddSessionExerciseRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        SessionExerciseDto response = sessionService.addSessionExercise(id, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Eliminar un ejercicio de la sesion",
            description = "Elimina el ejercicio de la sesion y sus series registradas. Devuelve 204 sin contenido.")
    @DeleteMapping("/sessions/{id}/exercises/{exerciseId}")
    public ResponseEntity<Void> removeSessionExercise(
            @PathVariable UUID id,
            @PathVariable UUID exerciseId,
            @RequestHeader("X-User-Id") UUID userId) {
        sessionService.removeSessionExercise(id, exerciseId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Registrar una serie en la sesion", description = "Calcula el volumen de la serie (peso x reps). Devuelve 201.")
    @PostMapping("/sessions/{id}/sets")
    public ResponseEntity<SetResponse> logSet(
            @PathVariable UUID id,
            @Valid @RequestBody LogSetRequest request) {
        SetResponse response = sessionService.logSet(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Obtener una sesion con sus series")
    @GetMapping("/sessions/{id}")
    public ResponseEntity<SessionResponse> getSession(@PathVariable UUID id) {
        SessionResponse response = sessionService.getSession(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Eliminar una sesion", description = "Devuelve 204 sin contenido.")
    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable UUID id) {
        sessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar sesiones del usuario", description = "Filtro opcional por rango de fechas (from/to, ISO-8601).")
    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponse>> getSessions(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<SessionResponse> response = sessionService.getSessions(userId, from, to);
        return ResponseEntity.ok(response);
    }
}
