package com.fittracker.calculator.presentation.controller;

import com.fittracker.calculator.presentation.dto.LoadRequest;
import com.fittracker.calculator.presentation.dto.LoadResponse;
import com.fittracker.calculator.presentation.dto.OneRepMaxRequest;
import com.fittracker.calculator.presentation.dto.OneRepMaxResponse;
import com.fittracker.calculator.presentation.dto.RoundLoadRequest;
import com.fittracker.calculator.presentation.dto.RoundLoadResponse;
import com.fittracker.calculator.service.interfaces.CalculatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/calculator")
@RequiredArgsConstructor
@Tag(name = "Calculator", description = "Calculos de 1RM, carga y redondeo de cargas")
public class CalculatorController {

    private final CalculatorService calculatorService;

    @Operation(summary = "Calcular el 1RM (one-rep-max)",
            description = "Estima la carga maxima a una repeticion con la formula de Epley: peso * (1 + reps/30).")
    @PostMapping("/one-rep-max")
    public ResponseEntity<OneRepMaxResponse> oneRepMax(@Valid @RequestBody OneRepMaxRequest request) {
        return ResponseEntity.ok(calculatorService.calculateOneRepMax(request.getWeight(), request.getReps()));
    }

    @Operation(summary = "Calcular carga para un objetivo",
            description = "Dada una serie de ejercicio y un objetivo de reps/max, calcula la carga sugerida.")
    @PostMapping("/load")
    public ResponseEntity<LoadResponse> load(@Valid @RequestBody LoadRequest request) {
        return ResponseEntity.ok(calculatorService.calculateLoad(
                request.getExerciseSet().getWeight(),
                request.getExerciseSet().getReps(),
                request.getGoal()
        ));
    }

    @Operation(summary = "Redondear carga al paso del equipamiento",
            description = "Ajusta la carga objetivo al incremento minimo permitido por el equipamiento (barra, mancuerna...).")
    @PostMapping("/round-load")
    public ResponseEntity<RoundLoadResponse> roundLoad(@Valid @RequestBody RoundLoadRequest request) {
        return ResponseEntity.ok(calculatorService.roundLoad(request.getTargetWeight(), request.getEquipment()));
    }

}
