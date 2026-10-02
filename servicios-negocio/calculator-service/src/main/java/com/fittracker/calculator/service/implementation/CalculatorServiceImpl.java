package com.fittracker.calculator.service.implementation;

import com.fittracker.calculator.persistence.entity.Equipment;
import com.fittracker.calculator.persistence.entity.TrainingGoal;
import com.fittracker.calculator.presentation.dto.LoadResponse;
import com.fittracker.calculator.presentation.dto.OneRepMaxResponse;
import com.fittracker.calculator.presentation.dto.RoundLoadResponse;
import com.fittracker.calculator.service.interfaces.CalculatorService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Service
public class CalculatorServiceImpl implements CalculatorService {

    private static final BigDecimal EPLEY_DIVISOR = new BigDecimal("30");
    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    // Fórmula de Epley: peso * (1 + repeticiones / 30)
    @Override
    public OneRepMaxResponse calculateOneRepMax(BigDecimal weight, Integer reps) {
        var multiplier = BigDecimal.ONE.add(
                new BigDecimal(reps).divide(EPLEY_DIVISOR, MC)
        );
        var oneRepMax = weight.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        return new OneRepMaxResponse(weight, reps, oneRepMax);
    }

    @Override
    public LoadResponse calculateLoad(BigDecimal weight, Integer reps, TrainingGoal goal) {
        var oneRepMax = calculateOneRepMax(weight, reps).getOneRepMax();
        return new LoadResponse(oneRepMax, goal, goal.getPercentage(), goal.applyTo(oneRepMax));
    }

    @Override
    public RoundLoadResponse roundLoad(BigDecimal targetWeight, Equipment equipment) {
        var increment = BigDecimal.valueOf(equipment.getIncrementKg());
        var steps = targetWeight.divide(increment, MC)
                .setScale(0, RoundingMode.HALF_UP);
        var rounded = steps.multiply(increment).setScale(2, RoundingMode.HALF_UP);
        return new RoundLoadResponse(equipment, increment, targetWeight, rounded);
    }

}
