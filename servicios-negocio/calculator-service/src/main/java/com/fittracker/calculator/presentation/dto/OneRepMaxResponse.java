package com.fittracker.calculator.presentation.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class OneRepMaxResponse {

    private final BigDecimal weight;
    private final Integer reps;
    private final BigDecimal oneRepMax;

}
