package com.fittracker.workout.service.implementation;

import com.fittracker.workout.persistence.entity.WorkoutSession;
import com.fittracker.workout.persistence.entity.WorkoutSet;
import com.fittracker.workout.persistence.entity.enums.MuscleLabel;
import com.fittracker.workout.persistence.entity.enums.ProgressionAction;
import com.fittracker.workout.persistence.entity.enums.SessionType;
import com.fittracker.workout.persistence.repository.WorkoutSessionRepository;
import com.fittracker.workout.persistence.repository.WorkoutSetRepository;
import com.fittracker.workout.service.http.CalculatorClient;
import com.fittracker.workout.service.http.RoutineClient;
import com.fittracker.workout.service.http.dto.OneRepMaxResponse;
import com.fittracker.workout.service.http.dto.RoundLoadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProgressionServiceImpl - Tests Unitarios")
class ProgressionServiceImplTest {

    @Mock private WorkoutSessionRepository sessionRepository;
    @Mock private WorkoutSetRepository setRepository;
    @Mock private CalculatorClient calculatorClient;
    @Mock private RoutineClient routineClient;

    @InjectMocks private ProgressionServiceImpl service;

    private final UUID userId = UUID.randomUUID();
    private final UUID exerciseId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "hypertrophyLow", 8);
        ReflectionTestUtils.setField(service, "hypertrophyHigh", 12);
        ReflectionTestUtils.setField(service, "rirLow", 1);
        ReflectionTestUtils.setField(service, "rirHigh", 3);
        ReflectionTestUtils.setField(service, "stableSessions", 2);
        ReflectionTestUtils.setField(service, "strengthRirMin", 1);
    }

    private WorkoutSession buildSession(SessionType type) {
        return WorkoutSession.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .performedAt(LocalDate.now())
                .sessionType(type)
                .muscleLabel(MuscleLabel.PUSH)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .sets(new java.util.ArrayList<>())
                .build();
    }

    private WorkoutSet buildSet(WorkoutSession session, int setNumber, int reps, Integer rir, String weight) {
        return WorkoutSet.builder()
                .session(session)
                .exerciseId(exerciseId)
                .exerciseName("Press banca")
                .setNumber(setNumber)
                .weightKg(new BigDecimal(weight))
                .reps(reps)
                .rir(rir)
                .build();
    }

    @Nested
    @DisplayName("getNextSessionType")
    class GetNextSessionType {

        @Test
        @DisplayName("Sin historial → HYPERTROPHY")
        void noHistory_returnsHypertrophy() {
            when(sessionRepository.findTopByUserIdAndMuscleLabelOrderByPerformedAtDesc(userId, MuscleLabel.PUSH))
                    .thenReturn(null);

            var result = service.getNextSessionType(userId, MuscleLabel.PUSH);

            assertThat(result).isEqualTo(SessionType.HYPERTROPHY);
        }

        @Test
        @DisplayName("Ultima STRENGTH → HYPERTROPHY (alterna)")
        void lastStrength_returnsHypertrophy() {
            when(sessionRepository.findTopByUserIdAndMuscleLabelOrderByPerformedAtDesc(userId, MuscleLabel.PUSH))
                    .thenReturn(buildSession(SessionType.STRENGTH));

            var result = service.getNextSessionType(userId, MuscleLabel.PUSH);

            assertThat(result).isEqualTo(SessionType.HYPERTROPHY);
        }

        @Test
        @DisplayName("Ultima HYPERTROPHY → STRENGTH (alterna)")
        void lastHypertrophy_returnsStrength() {
            when(sessionRepository.findTopByUserIdAndMuscleLabelOrderByPerformedAtDesc(userId, MuscleLabel.PUSH))
                    .thenReturn(buildSession(SessionType.HYPERTROPHY));

            var result = service.getNextSessionType(userId, MuscleLabel.PUSH);

            assertThat(result).isEqualTo(SessionType.STRENGTH);
        }
    }

    @Nested
    @DisplayName("getRecommendation - HYPERTROPHY")
    class HypertrophyRecommendation {

        private WorkoutSession lastSession() {
            WorkoutSession session = buildSession(SessionType.HYPERTROPHY);
            when(sessionRepository.findTopByUserIdAndMuscleLabelOrderByPerformedAtDesc(userId, MuscleLabel.PUSH))
                    .thenReturn(session);
            return session;
        }

        @Test
        @DisplayName("Sin sesion previa → recomendacion vacia")
        void noHistory_emptyRecommendations() {
            when(sessionRepository.findTopByUserIdAndMuscleLabelOrderByPerformedAtDesc(userId, MuscleLabel.PUSH))
                    .thenReturn(null);

            var result = service.getRecommendation(userId, MuscleLabel.PUSH);

            assertThat(result.items()).isEmpty();
            assertThat(result.nextSessionType()).isEqualTo(SessionType.HYPERTROPHY);
        }

        @Test
        @DisplayName("Todas 12 reps con RIR 1-3 → INCREASE_LOAD +2.5")
        void allAtTop_increaseLoad() {
            WorkoutSession session = lastSession();
            when(setRepository.findBySessionId(session.getId())).thenReturn(List.of(
                    buildSet(session, 1, 12, 2, "60"),
                    buildSet(session, 2, 12, 1, "60"),
                    buildSet(session, 3, 12, 3, "60")));

            var result = service.getRecommendation(userId, MuscleLabel.PUSH);

            assertThat(result.items()).hasSize(1);
            var rec = result.items().get(0);
            assertThat(rec.action()).isEqualTo(ProgressionAction.INCREASE_LOAD);
            assertThat(rec.currentLoad()).isEqualByComparingTo("60");
            assertThat(rec.suggestedLoad()).isEqualByComparingTo("62.5");
        }

        @Test
        @DisplayName("Reps en rango 8-12 con RIR ok → MAINTAIN")
        void allInRange_maintain() {
            WorkoutSession session = lastSession();
            when(setRepository.findBySessionId(session.getId())).thenReturn(List.of(
                    buildSet(session, 1, 10, 2, "60"),
                    buildSet(session, 2, 9, 3, "60")));

            var result = service.getRecommendation(userId, MuscleLabel.PUSH);

            var rec = result.items().get(0);
            assertThat(rec.action()).isEqualTo(ProgressionAction.MAINTAIN);
            assertThat(rec.suggestedLoad()).isEqualByComparingTo("60");
        }

        @Test
        @DisplayName("Reps por debajo de 8 → DECREASE_LOAD")
        void belowRange_decreaseLoad() {
            WorkoutSession session = lastSession();
            when(setRepository.findBySessionId(session.getId())).thenReturn(List.of(
                    buildSet(session, 1, 5, 2, "60"),
                    buildSet(session, 2, 10, 2, "60")));

            var result = service.getRecommendation(userId, MuscleLabel.PUSH);

            var rec = result.items().get(0);
            assertThat(rec.action()).isEqualTo(ProgressionAction.DECREASE_LOAD);
            assertThat(rec.suggestedLoad()).isEqualByComparingTo("57.5");
        }

        @Test
        @DisplayName("RIR 0 (fallo) → DECREASE_LOAD")
        void rirZero_decreaseLoad() {
            WorkoutSession session = lastSession();
            when(setRepository.findBySessionId(session.getId())).thenReturn(List.of(
                    buildSet(session, 1, 10, 0, "60")));

            var result = service.getRecommendation(userId, MuscleLabel.PUSH);

            assertThat(result.items().get(0).action()).isEqualTo(ProgressionAction.DECREASE_LOAD);
        }
    }

    @Nested
    @DisplayName("getRecommendation - STRENGTH")
    class StrengthRecommendation {

        @Test
        @DisplayName("No cumple 5 reps → MAINTAIN")
        void belowTarget_maintain() {
            WorkoutSession session = buildSession(SessionType.STRENGTH);
            when(sessionRepository.findTopByUserIdAndMuscleLabelOrderByPerformedAtDesc(userId, MuscleLabel.PUSH))
                    .thenReturn(session);
            when(setRepository.findBySessionId(session.getId())).thenReturn(List.of(
                    buildSet(session, 1, 3, 2, "100")));

            var result = service.getRecommendation(userId, MuscleLabel.PUSH);

            assertThat(result.items().get(0).action()).isEqualTo(ProgressionAction.MAINTAIN);
        }

        @Test
        @DisplayName("Cumple pero solo 1 sesion estable → MAINTAIN (aun no alcanza)")
        void notEnoughStableSessions_maintain() {
            WorkoutSession session = buildSession(SessionType.STRENGTH);
            when(sessionRepository.findTopByUserIdAndMuscleLabelOrderByPerformedAtDesc(userId, MuscleLabel.PUSH))
                    .thenReturn(session);
            when(setRepository.findBySessionId(session.getId())).thenReturn(List.of(
                    buildSet(session, 1, 5, 2, "100")));
            when(sessionRepository.findByUserIdAndMuscleLabelAndPerformedAtBetween(
                    any(), any(), any(), any())).thenReturn(List.of());

            var result = service.getRecommendation(userId, MuscleLabel.PUSH);

            assertThat(result.items().get(0).action()).isEqualTo(ProgressionAction.MAINTAIN);
        }

        @Test
        @DisplayName("Estable 2 sesiones + calculator disponible → INCREASE_LOAD")
        void stableWithCalculator_increaseLoad() {
            WorkoutSession session = buildSession(SessionType.STRENGTH);
            when(sessionRepository.findTopByUserIdAndMuscleLabelOrderByPerformedAtDesc(userId, MuscleLabel.PUSH))
                    .thenReturn(session);
            when(setRepository.findBySessionId(session.getId())).thenReturn(List.of(
                    buildSet(session, 1, 5, 2, "100")));

            WorkoutSession past = buildSession(SessionType.STRENGTH);
            when(sessionRepository.findByUserIdAndMuscleLabelAndPerformedAtBetween(
                    any(), any(), any(), any())).thenReturn(List.of(past));
            when(setRepository.findBySessionId(past.getId())).thenReturn(List.of(
                    buildSet(past, 1, 5, 2, "100")));

            when(calculatorClient.oneRepMax(any())).thenReturn(new OneRepMaxResponse(new BigDecimal("116.7")));
            when(calculatorClient.roundLoad(any())).thenReturn(new RoundLoadResponse(new BigDecimal("102.5")));

            var result = service.getRecommendation(userId, MuscleLabel.PUSH);

            var rec = result.items().get(0);
            assertThat(rec.action()).isEqualTo(ProgressionAction.INCREASE_LOAD);
            assertThat(rec.suggestedLoad()).isEqualByComparingTo("102.5");
        }

        @Test
        @DisplayName("Estable pero calculator caido → MAINTAIN (fallback)")
        void calculatorDown_maintain() {
            WorkoutSession session = buildSession(SessionType.STRENGTH);
            when(sessionRepository.findTopByUserIdAndMuscleLabelOrderByPerformedAtDesc(userId, MuscleLabel.PUSH))
                    .thenReturn(session);
            when(setRepository.findBySessionId(session.getId())).thenReturn(List.of(
                    buildSet(session, 1, 5, 2, "100")));

            WorkoutSession past = buildSession(SessionType.STRENGTH);
            when(sessionRepository.findByUserIdAndMuscleLabelAndPerformedAtBetween(
                    any(), any(), any(), any())).thenReturn(List.of(past));
            when(setRepository.findBySessionId(past.getId())).thenReturn(List.of(
                    buildSet(past, 1, 5, 2, "100")));

            when(calculatorClient.oneRepMax(any())).thenThrow(new RuntimeException("service down"));

            var result = service.getRecommendation(userId, MuscleLabel.PUSH);

            assertThat(result.items().get(0).action()).isEqualTo(ProgressionAction.MAINTAIN);
        }
    }
}
