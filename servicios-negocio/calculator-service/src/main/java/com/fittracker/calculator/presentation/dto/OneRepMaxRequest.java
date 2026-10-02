package com.fittracker.calculator.presentation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OneRepMaxRequest {

    @NotNull(message = "el peso es obligatorio")
    @DecimalMin(value = "0.1", message = "el peso debe ser mayor que 0")
    private BigDecimal weight;

    @NotNull(message = "las repeticiones son obligatorias")
    @Min(value = 1, message = "las repeticiones deben ser al menos 1")
    @Max(value = 20, message = "la fórmula de 1RM pierde precisión por encima de 20 repeticiones")
    private Integer reps;

}
