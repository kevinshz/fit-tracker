package com.fittracker.workout.service.implementation;

import com.fittracker.workout.persistence.entity.WorkoutSession;
import com.fittracker.workout.persistence.entity.WorkoutSet;
import com.fittracker.workout.persistence.entity.enums.MuscleLabel;
import com.fittracker.workout.persistence.entity.enums.ProgressionAction;
import com.fittracker.workout.persistence.entity.enums.SessionType;
import com.fittracker.workout.persistence.repository.WorkoutSessionRepository;
import com.fittracker.workout.persistence.repository.WorkoutSetRepository;
import com.fittracker.workout.presentation.dto.ExerciseRecommendation;
import com.fittracker.workout.presentation.dto.ProgressionRecommendation;
import com.fittracker.workout.service.http.CalculatorClient;
import com.fittracker.workout.service.http.RoutineClient;
import com.fittracker.workout.service.http.dto.ExerciseSummaryDto;
import com.fittracker.workout.service.http.dto.OneRepMaxRequest;
import com.fittracker.workout.service.http.dto.OneRepMaxResponse;
import com.fittracker.workout.service.http.dto.RoundLoadRequest;
import com.fittracker.workout.service.http.dto.RoundLoadResponse;
import com.fittracker.workout.service.interfaces.ProgressionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProgressionServiceImpl implements ProgressionService {

    private final WorkoutSessionRepository sessionRepository;
    private final WorkoutSetRepository setRepository;
    private final CalculatorClient calculatorClient;
    private final RoutineClient routineClient;

    @Value("${fit-tracker.progression.hypertrophy.low:8}")
    private int hypertrophyLow;

    @Value("${fit-tracker.progression.hypertrophy.high:12}")
    private int hypertrophyHigh;

    @Value("${fit-tracker.progression.hypertrophy.rir-low:1}")
    private int rirLow;

    @Value("${fit-tracker.progression.hypertrophy.rir-high:3}")
    private int rirHigh;

    @Value("${fit-tracker.progression.strength.stable-sessions:2}")
    private int stableSessions;

    @Value("${fit-tracker.progression.strength.rir-min:1}")
    private int strengthRirMin;

    @Override
    @Transactional(readOnly = true)
    public SessionType getNextSessionType(UUID userId, MuscleLabel muscleLabel) {
        WorkoutSession lastSession = sessionRepository.findTopByUserIdAndMuscleLabelOrderByPerformedAtDesc(userId, muscleLabel);
        if (lastSession == null) {
            return SessionType.HYPERTROPHY;
        }
        return lastSession.getSessionType() == SessionType.STRENGTH
                ? SessionType.HYPERTROPHY
                : SessionType.STRENGTH;
    }

    @Override
    @Transactional(readOnly = true)
    public ProgressionRecommendation getRecommendation(UUID userId, MuscleLabel muscleLabel) {
        SessionType nextType = getNextSessionType(userId, muscleLabel);

        WorkoutSession lastSession = sessionRepository.findTopByUserIdAndMuscleLabelOrderByPerformedAtDesc(userId, muscleLabel);
        if (lastSession == null) {
            return new ProgressionRecommendation(nextType, muscleLabel, List.of());
        }

        List<WorkoutSet> lastSets = setRepository.findBySessionId(lastSession.getId());
        Map<UUID, List<WorkoutSet>> setsByExercise = lastSets.stream()
                .collect(Collectors.groupingBy(WorkoutSet::getExerciseId));

        List<ExerciseRecommendation> recommendations = new ArrayList<>();

        for (Map.Entry<UUID, List<WorkoutSet>> entry : setsByExercise.entrySet()) {
            UUID exerciseId = entry.getKey();
            List<WorkoutSet> exerciseSets = entry.getValue();
            String exerciseName = exerciseSets.get(0).getExerciseName();

            ExerciseRecommendation rec = evaluateExercise(exerciseId, exerciseName, exerciseSets, lastSession.getSessionType());
            recommendations.add(rec);
        }

        return new ProgressionRecommendation(nextType, muscleLabel, recommendations);
    }

    private ExerciseRecommendation evaluateExercise(UUID exerciseId, String exerciseName,
                                                     List<WorkoutSet> sets, SessionType currentType) {
        if (sets == null || sets.isEmpty()) {
            return new ExerciseRecommendation(exerciseId, exerciseName, ProgressionAction.MAINTAIN,
                    BigDecimal.ZERO, BigDecimal.ZERO, "Sin datos para evaluar");
        }

        BigDecimal currentLoad = sets.get(0).getWeightKg();

        if (currentType == SessionType.HYPERTROPHY) {
            return evaluateHypertrophy(exerciseId, exerciseName, sets, currentLoad);
        } else {
            return evaluateStrength(exerciseId, exerciseName, sets, currentLoad);
        }
    }

    private ExerciseRecommendation evaluateHypertrophy(UUID exerciseId, String exerciseName,
                                                        List<WorkoutSet> sets, BigDecimal currentLoad) {
        boolean allAtTop = sets.stream().allMatch(s ->
                s.getReps() == hypertrophyHigh && s.getRir() != null
                        && s.getRir() >= rirLow && s.getRir() <= rirHigh);

        boolean allInRange = sets.stream().allMatch(s ->
                s.getReps() >= hypertrophyLow && s.getReps() <= hypertrophyHigh
                        && s.getRir() != null && s.getRir() >= rirLow && s.getRir() <= rirHigh);

        boolean anyBelow = sets.stream().anyMatch(s ->
                s.getReps() < hypertrophyLow || (s.getRir() != null && s.getRir() < rirLow));

        if (allAtTop) {
            BigDecimal suggested = currentLoad.add(BigDecimal.valueOf(2.5));
            suggested = roundToIncrement(suggested, "BARBELL");
            return new ExerciseRecommendation(exerciseId, exerciseName, ProgressionAction.INCREASE_LOAD,
                    currentLoad, suggested,
                    String.format("Todas las series en %d reps con RIR %d-%d → aumentar carga",
                            hypertrophyHigh, rirLow, rirHigh));
        }

        if (anyBelow) {
            BigDecimal suggested = currentLoad.subtract(BigDecimal.valueOf(2.5));
            if (suggested.compareTo(BigDecimal.ZERO) < 0) suggested = currentLoad;
            return new ExerciseRecommendation(exerciseId, exerciseName, ProgressionAction.DECREASE_LOAD,
                    currentLoad, suggested,
                    String.format("Reps por debajo de %d o RIR < %d → reducir carga", hypertrophyLow, rirLow));
        }

        if (allInRange) {
            return new ExerciseRecommendation(exerciseId, exerciseName, ProgressionAction.MAINTAIN,
                    currentLoad, currentLoad,
                    String.format("Reps en rango %d-%d con RIR %d-%d → mantener carga",
                            hypertrophyLow, hypertrophyHigh, rirLow, rirHigh));
        }

        return new ExerciseRecommendation(exerciseId, exerciseName, ProgressionAction.MAINTAIN,
                currentLoad, currentLoad, "Rendimiento mixto → mantener carga y consolidar");
    }

    private ExerciseRecommendation evaluateStrength(UUID exerciseId, String exerciseName,
                                                     List<WorkoutSet> sets, BigDecimal currentLoad) {
        int targetReps = 5;
        boolean currentOk = sets.stream().allMatch(s ->
                s.getReps() >= targetReps && s.getRir() != null && s.getRir() >= strengthRirMin);

        if (!currentOk) {
            return new ExerciseRecommendation(exerciseId, exerciseName, ProgressionAction.MAINTAIN,
                    currentLoad, currentLoad,
                    String.format("No cumple objetivo %d reps con RIR >= %d → mantener",
                            targetReps, strengthRirMin));
        }

        // Verificar estabilidad con sesiones anteriores
        WorkoutSession lastSession = sets.get(0).getSession();
        List<WorkoutSession> history = sessionRepository
                .findByUserIdAndMuscleLabelAndPerformedAtBetween(
                        lastSession.getUserId(), lastSession.getMuscleLabel(),
                        lastSession.getPerformedAt().minusDays(30), lastSession.getPerformedAt().minusDays(1));

        int stableCount = 1;
        for (WorkoutSession pastSession : history) {
            List<WorkoutSet> pastSets = setRepository.findBySessionId(pastSession.getId());
            boolean pastOk = pastSets.stream().allMatch(s ->
                    s.getReps() >= targetReps && s.getRir() != null && s.getRir() >= strengthRirMin);
            if (pastOk) {
                stableCount++;
            } else {
                break;
            }
        }

        if (stableCount >= stableSessions) {
            try {
                int avgReps = (int) sets.stream().mapToInt(WorkoutSet::getReps).average().orElse(targetReps);
                OneRepMaxResponse oneRmResp = calculatorClient.oneRepMax(new OneRepMaxRequest(currentLoad, avgReps));
                BigDecimal oneRm = oneRmResp.oneRepMax();
                BigDecimal target85 = oneRm.multiply(new BigDecimal("0.85")).setScale(1, RoundingMode.HALF_UP);

                RoundLoadResponse rounded = calculatorClient.roundLoad(new RoundLoadRequest(target85, "BARBELL"));
                BigDecimal recommended = rounded.roundedWeight().compareTo(currentLoad) > 0
                        ? rounded.roundedWeight() : currentLoad;
                ProgressionAction action = recommended.compareTo(currentLoad) > 0
                        ? ProgressionAction.INCREASE_LOAD : ProgressionAction.MAINTAIN;

                return new ExerciseRecommendation(exerciseId, exerciseName, action,
                        currentLoad, recommended,
                        String.format("Estable %d sesiones con %d reps RIR>=%d → 1RM %.1f → 85%% = %.1f",
                                stableCount, targetReps, strengthRirMin, oneRm, target85));
            } catch (Exception e) {
                log.warn("Calculator no disponible para fuerza, manteniendo: {}", e.getMessage());
                return new ExerciseRecommendation(exerciseId, exerciseName, ProgressionAction.MAINTAIN,
                        currentLoad, currentLoad, "Estable pero calculator no disponible → mantener (fallback)");
            }
        }

        return new ExerciseRecommendation(exerciseId, exerciseName, ProgressionAction.MAINTAIN,
                currentLoad, currentLoad,
                String.format("Cumple objetivo pero requiere %d sesiones estables (lleva %d) → mantener",
                        stableSessions, stableCount));
    }

    private BigDecimal roundToIncrement(BigDecimal weight, String equipment) {
        BigDecimal increment = BigDecimal.valueOf(2.5);
        return weight.divide(increment, 0, RoundingMode.HALF_UP).multiply(increment);
    }
}
