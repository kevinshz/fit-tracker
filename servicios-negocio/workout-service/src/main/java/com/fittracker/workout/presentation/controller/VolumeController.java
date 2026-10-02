package com.fittracker.workout.presentation.controller;

import com.fittracker.workout.presentation.dto.VolumeReportDto;
import com.fittracker.workout.service.interfaces.VolumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workouts/volume")
@RequiredArgsConstructor
@Tag(name = "Volume", description = "Reportes de volumen entrenado")
public class VolumeController {

    private final VolumeService volumeService;

    @Operation(summary = "Reporte de volumen por rango de fechas", description = "Agrupa el volumen total (kg) por musculo o por sesion. Requiere from y to (ISO-8601).")
    @GetMapping("/report")
    public ResponseEntity<VolumeReportDto> getVolumeReport(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "muscle") String groupBy) {
        VolumeReportDto response = volumeService.getUserVolume(userId, from, to, groupBy);
        return ResponseEntity.ok(response);
    }
}
