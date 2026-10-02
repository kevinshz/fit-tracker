package com.fittracker.calculator.presentation.dto;

import com.fittracker.calculator.persistence.entity.TrainingGoal;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class LoadResponse {

    private final BigDecimal oneRepMax;
    private final TrainingGoal goal;
    private final BigDecimal percentageApplied;
    private final BigDecimal rawLoad;

}
