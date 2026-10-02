package com.fittracker.workout.service;

import com.fittracker.workout.persistence.entity.WorkoutSession;
import com.fittracker.workout.persistence.entity.enums.MuscleLabel;
import com.fittracker.workout.persistence.entity.enums.SessionType;
import com.fittracker.workout.persistence.repository.WorkoutSessionRepository;
import com.fittracker.workout.service.implementation.ProgressionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DupAlternanceResolverTest {

    @Mock
    WorkoutSessionRepository sessionRepository;

    @InjectMocks
    ProgressionServiceImpl progressionService;

    @Test
    void primeraVez_retornaHypertrophy() {
        when(sessionRepository.findTopByUserIdAndMuscleLabelOrderByPerformedAtDesc(
                any(UUID.class), any(MuscleLabel.class)))
                .thenReturn(null);

        SessionType result = progressionService.getNextSessionType(UUID.randomUUID(), MuscleLabel.CHEST_BACK);

        assertEquals(SessionType.HYPERTROPHY, result);
    }

    @Test
    void despuesDeStrength_retornaHypertrophy() {
        UUID userId = UUID.randomUUID();
        WorkoutSession lastSession = WorkoutSession.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .muscleLabel(MuscleLabel.CHEST_BACK)
                .sessionType(SessionType.STRENGTH)
                .build();

        when(sessionRepository.findTopByUserIdAndMuscleLabelOrderByPerformedAtDesc(userId, MuscleLabel.CHEST_BACK))
                .thenReturn(lastSession);

        SessionType result = progressionService.getNextSessionType(userId, MuscleLabel.CHEST_BACK);

        assertEquals(SessionType.HYPERTROPHY, result);
    }

    @Test
    void despuesDeHypertrophy_retornaStrength() {
        UUID userId = UUID.randomUUID();
        WorkoutSession lastSession = WorkoutSession.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .muscleLabel(MuscleLabel.LEGS)
                .sessionType(SessionType.HYPERTROPHY)
                .build();

        when(sessionRepository.findTopByUserIdAndMuscleLabelOrderByPerformedAtDesc(userId, MuscleLabel.LEGS))
                .thenReturn(lastSession);

        SessionType result = progressionService.getNextSessionType(userId, MuscleLabel.LEGS);

        assertEquals(SessionType.STRENGTH, result);
    }

    @Test
    void alternanciaEstricta_strengthLuegoHypertrophyLuegoStrength() {
        UUID userId = UUID.randomUUID();

        // Primera交替: STRENGTH → HYPERTROPHY
        WorkoutSession session1 = WorkoutSession.builder()
                .id(UUID.randomUUID()).userId(userId)
                .muscleLabel(MuscleLabel.PUSH).sessionType(SessionType.STRENGTH).build();
        when(sessionRepository.findTopByUserIdAndMuscleLabelOrderByPerformedAtDesc(userId, MuscleLabel.PUSH))
                .thenReturn(session1);
        assertEquals(SessionType.HYPERTROPHY, progressionService.getNextSessionType(userId, MuscleLabel.PUSH));

        // Segunda交替: HYPERTROPHY → STRENGTH
        WorkoutSession session2 = WorkoutSession.builder()
                .id(UUID.randomUUID()).userId(userId)
                .muscleLabel(MuscleLabel.PUSH).sessionType(SessionType.HYPERTROPHY).build();
        when(sessionRepository.findTopByUserIdAndMuscleLabelOrderByPerformedAtDesc(userId, MuscleLabel.PUSH))
                .thenReturn(session2);
        assertEquals(SessionType.STRENGTH, progressionService.getNextSessionType(userId, MuscleLabel.PUSH));
    }

    @Test
    void diferentesMuscleLabelsIndependientes() {
        UUID userId = UUID.randomUUID();

        WorkoutSession chestSession = WorkoutSession.builder()
                .id(UUID.randomUUID()).userId(userId)
                .muscleLabel(MuscleLabel.CHEST_BACK).sessionType(SessionType.STRENGTH).build();
        WorkoutSession legsSession = WorkoutSession.builder()
                .id(UUID.randomUUID()).userId(userId)
                .muscleLabel(MuscleLabel.LEGS).sessionType(SessionType.HYPERTROPHY).build();

        when(sessionRepository.findTopByUserIdAndMuscleLabelOrderByPerformedAtDesc(userId, MuscleLabel.CHEST_BACK))
                .thenReturn(chestSession);
        when(sessionRepository.findTopByUserIdAndMuscleLabelOrderByPerformedAtDesc(userId, MuscleLabel.LEGS))
                .thenReturn(legsSession);

        assertEquals(SessionType.HYPERTROPHY, progressionService.getNextSessionType(userId, MuscleLabel.CHEST_BACK));
        assertEquals(SessionType.STRENGTH, progressionService.getNextSessionType(userId, MuscleLabel.LEGS));
    }
}
