package com.fittracker.workout.presentation.controller;

import com.fittracker.workout.persistence.entity.enums.MuscleLabel;
import com.fittracker.workout.presentation.dto.ProgressionRecommendation;
import com.fittracker.workout.service.interfaces.ProgressionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workouts/progression")
@RequiredArgsConstructor
@Tag(name = "Progression", description = "Recomendaciones de progresion y alternancia")
public class ProgressionController {

    private final ProgressionService progressionService;

    @Operation(summary = "Recomendacion de progresion", description = "Sugerencia de carga/reps para el siguiente entreno de un grupo muscular.")
    @GetMapping("/recommendation")
    public ResponseEntity<ProgressionRecommendation> getRecommendation(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam MuscleLabel muscleLabel) {
        ProgressionRecommendation response = progressionService.getRecommendation(userId, muscleLabel);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Siguiente tipo de sesion", description = "Alterna Fuerza/Hipertrofia segun el historial reciente del grupo muscular.")
    @GetMapping("/next-type")
    public ResponseEntity<Map<String, String>> getNextSessionType(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam MuscleLabel muscleLabel) {
        var nextType = progressionService.getNextSessionType(userId, muscleLabel);
        return ResponseEntity.ok(Map.of(
                "muscleLabel", muscleLabel.name(),
                "nextSessionType", nextType.name()
        ));
    }
}
