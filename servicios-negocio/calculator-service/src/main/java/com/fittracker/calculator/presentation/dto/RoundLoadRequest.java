package com.fittracker.calculator.presentation.dto;

import com.fittracker.calculator.persistence.entity.Equipment;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RoundLoadRequest {

    @NotNull(message = "el peso objetivo es obligatorio")
    @DecimalMin(value = "0.1", message = "el peso objetivo debe ser mayor que 0")
    private BigDecimal targetWeight;

    @NotNull(message = "el equipamiento es obligatorio")
    private Equipment equipment;

}
