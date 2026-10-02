package com.fittracker.calculator.presentation.dto;

import com.fittracker.calculator.persistence.entity.TrainingGoal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoadRequest {

    @Valid
    @NotNull(message = "los datos del levantamiento son obligatorios")
    private OneRepMaxRequest exerciseSet;

    @NotNull(message = "el objetivo de entrenamiento es obligatorio")
    private TrainingGoal goal;

}
