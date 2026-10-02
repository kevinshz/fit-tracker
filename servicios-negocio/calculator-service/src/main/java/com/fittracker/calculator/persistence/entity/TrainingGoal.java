package com.fittracker.calculator.persistence.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@RequiredArgsConstructor
public enum TrainingGoal {

    STRENGTH(new BigDecimal("0.85")),
    HYPERTROPHY(new BigDecimal("0.70"));

    private final BigDecimal percentage;

    public BigDecimal applyTo(BigDecimal oneRepMax) {
        return oneRepMax.multiply(percentage).setScale(2, RoundingMode.HALF_UP);
    }

}
