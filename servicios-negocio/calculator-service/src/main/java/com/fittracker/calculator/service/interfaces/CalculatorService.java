package com.fittracker.calculator.service.interfaces;

import com.fittracker.calculator.persistence.entity.Equipment;
import com.fittracker.calculator.persistence.entity.TrainingGoal;
import com.fittracker.calculator.presentation.dto.LoadResponse;
import com.fittracker.calculator.presentation.dto.OneRepMaxResponse;
import com.fittracker.calculator.presentation.dto.RoundLoadResponse;

import java.math.BigDecimal;

public interface CalculatorService {

    OneRepMaxResponse calculateOneRepMax(BigDecimal weight, Integer reps);

    LoadResponse calculateLoad(BigDecimal weight, Integer reps, TrainingGoal goal);

    RoundLoadResponse roundLoad(BigDecimal targetWeight, Equipment equipment);

}
