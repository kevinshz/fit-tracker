package com.fittracker.calculator.presentation.dto;

import com.fittracker.calculator.persistence.entity.Equipment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class RoundLoadResponse {

    private final Equipment equipment;
    private final BigDecimal incrementKg;
    private final BigDecimal rawWeight;
    private final BigDecimal roundedWeight;

}
