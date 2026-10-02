package com.fittracker.workout.persistence.repository;

import com.fittracker.workout.AbstractPostgresIntegrationTest;
import com.fittracker.workout.persistence.entity.WorkoutSession;
import com.fittracker.workout.persistence.entity.WorkoutSet;
import com.fittracker.workout.persistence.entity.enums.MuscleLabel;
import com.fittracker.workout.persistence.entity.enums.SessionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@EnabledIfSystemProperty(named = "testcontainers.enabled", matches = "true",
        disabledReason = "Requiere Docker (usar -Dtestcontainers.enabled=true)")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("WorkoutSession/Set Repository - Integracion con PostgreSQL (Testcontainers)")
class WorkoutRepositoryTest extends AbstractPostgresIntegrationTest {

    @Autowired private WorkoutSessionRepository sessionRepository;
    @Autowired private WorkoutSetRepository setRepository;

    private final UUID userId = UUID.randomUUID();

    private WorkoutSession buildSession(LocalDate date, MuscleLabel label, SessionType type) {
        return WorkoutSession.builder()
                .userId(userId)
                .performedAt(date)
                .sessionType(type)
                .muscleLabel(label)
                .sets(new ArrayList<>())
                .build();
    }

    private WorkoutSet buildSet(WorkoutSession session, int setNumber, String weight, int reps) {
        return WorkoutSet.builder()
                .session(session)
                .exerciseId(UUID.randomUUID())
                .exerciseName("Press banca")
                .setNumber(setNumber)
                .weightKg(new BigDecimal(weight))
                .reps(reps)
                .rir(2)
                .volumeKg(new BigDecimal(weight).multiply(BigDecimal.valueOf(reps)))
                .build();
    }

    @Test
    @DisplayName("Sesion con sets persiste via cascade")
    void sessionWithSets_cascadePersist() {
        WorkoutSession session = buildSession(LocalDate.now(), MuscleLabel.CHEST_BACK, SessionType.HYPERTROPHY);
        session.getSets().add(buildSet(session, 1, "60", 8));
        session.getSets().add(buildSet(session, 2, "60", 8));
        sessionRepository.save(session);

        var found = sessionRepository.findById(session.getId()).orElseThrow();

        assertThat(found.getSets()).hasSize(2);
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByUserIdOrderByPerformedAtDesc ordena desc")
    void orderByPerformedAtDesc() {
        sessionRepository.save(buildSession(LocalDate.now().minusDays(10), MuscleLabel.LEGS, SessionType.STRENGTH));
        sessionRepository.save(buildSession(LocalDate.now(), MuscleLabel.LEGS, SessionType.STRENGTH));
        sessionRepository.save(buildSession(LocalDate.now().minusDays(5), MuscleLabel.LEGS, SessionType.STRENGTH));

        var result = sessionRepository.findByUserIdOrderByPerformedAtDesc(userId);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getPerformedAt()).isEqualTo(LocalDate.now());
        assertThat(result.get(2).getPerformedAt()).isEqualTo(LocalDate.now().minusDays(10));
    }

    @Test
    @DisplayName("findTopByUserIdAndMuscleLabel retorna la ultima del grupo muscular")
    void findTopByMuscleLabel() {
        sessionRepository.save(buildSession(LocalDate.now().minusDays(7), MuscleLabel.LEGS, SessionType.STRENGTH));
        sessionRepository.save(buildSession(LocalDate.now(), MuscleLabel.LEGS, SessionType.HYPERTROPHY));
        sessionRepository.save(buildSession(LocalDate.now(), MuscleLabel.PUSH, SessionType.STRENGTH));

        var top = sessionRepository.findTopByUserIdAndMuscleLabelOrderByPerformedAtDesc(userId, MuscleLabel.LEGS);

        assertThat(top).isNotNull();
        assertThat(top.getSessionType()).isEqualTo(SessionType.HYPERTROPHY);
    }

    @Test
    @DisplayName("findByUserIdAndPerformedAtBetween filtra por rango")
    void findByDateRange() {
        sessionRepository.save(buildSession(LocalDate.now().minusDays(30), MuscleLabel.LEGS, SessionType.STRENGTH));
        sessionRepository.save(buildSession(LocalDate.now().minusDays(3), MuscleLabel.LEGS, SessionType.STRENGTH));
        sessionRepository.save(buildSession(LocalDate.now(), MuscleLabel.LEGS, SessionType.STRENGTH));

        var result = sessionRepository.findByUserIdAndPerformedAtBetween(
                userId, LocalDate.now().minusDays(7), LocalDate.now());

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("findTop retorna null sin historial")
    void findTop_noHistory_returnsNull() {
        assertThat(sessionRepository.findTopByUserIdAndMuscleLabelOrderByPerformedAtDesc(
                UUID.randomUUID(), MuscleLabel.PUSH)).isNull();
    }

    @Test
    @DisplayName("findBySessionId retorna sets de la sesion")
    void findBySessionId() {
        WorkoutSession session = buildSession(LocalDate.now(), MuscleLabel.PUSH, SessionType.STRENGTH);
        session.getSets().add(buildSet(session, 1, "80", 5));
        session.getSets().add(buildSet(session, 2, "80", 5));
        session.getSets().add(buildSet(session, 3, "82.5", 5));
        sessionRepository.save(session);

        List<WorkoutSet> sets = setRepository.findBySessionId(session.getId());

        assertThat(sets).hasSize(3);
        assertThat(sets.get(0).getSetNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("Consulta agregada de volumen por ejercicio")
    void aggregateVolumeByExercise() {
        WorkoutSession session = buildSession(LocalDate.now(), MuscleLabel.PUSH, SessionType.STRENGTH);
        UUID exerciseId = UUID.randomUUID();
        WorkoutSet set1 = buildSet(session, 1, "100", 5);
        set1.setExerciseId(exerciseId);
        WorkoutSet set2 = buildSet(session, 2, "100", 5);
        set2.setExerciseId(exerciseId);
        session.getSets().add(set1);
        session.getSets().add(set2);
        sessionRepository.save(session);

        var result = setRepository.aggregateVolumeByExercise(
                userId, LocalDate.now().minusDays(1), LocalDate.now());

        assertThat(result).hasSize(1);
        assertThat((BigDecimal) result.get(0)[2]).isEqualByComparingTo("1000");
        assertThat((Long) result.get(0)[3]).isEqualTo(2L);
    }

    @Test
    @DisplayName("sumVolumeByUserId suma todo el volumen")
    void sumVolumeByUserId() {
        WorkoutSession session = buildSession(LocalDate.now(), MuscleLabel.PUSH, SessionType.STRENGTH);
        session.getSets().add(buildSet(session, 1, "60", 10));
        session.getSets().add(buildSet(session, 2, "60", 10));
        sessionRepository.save(session);

        BigDecimal total = setRepository.sumVolumeByUserId(userId);

        assertThat(total).isEqualByComparingTo("1200");
    }

    @Test
    @DisplayName("Borrar sesion borra sus sets (orphanRemoval)")
    void deleteSession_deletesSets() {
        WorkoutSession session = buildSession(LocalDate.now(), MuscleLabel.PUSH, SessionType.STRENGTH);
        session.getSets().add(buildSet(session, 1, "60", 10));
        sessionRepository.save(session);
        UUID sessionId = session.getId();

        sessionRepository.deleteById(sessionId);

        assertThat(sessionRepository.findById(sessionId)).isEmpty();
        assertThat(setRepository.findBySessionId(sessionId)).isEmpty();
    }
}
