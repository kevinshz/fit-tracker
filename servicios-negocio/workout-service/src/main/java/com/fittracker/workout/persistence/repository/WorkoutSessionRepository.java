package com.fittracker.workout.persistence.repository;

import com.fittracker.workout.persistence.entity.WorkoutSession;
import com.fittracker.workout.persistence.entity.enums.MuscleLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, UUID> {

    List<WorkoutSession> findByUserIdOrderByPerformedAtDesc(UUID userId);

    WorkoutSession findTopByUserIdAndMuscleLabelOrderByPerformedAtDesc(UUID userId, MuscleLabel muscleLabel);

    List<WorkoutSession> findByUserIdAndPerformedAtBetween(UUID userId, LocalDate from, LocalDate to);

    List<WorkoutSession> findByUserIdAndMuscleLabelAndPerformedAtBetween(UUID userId, MuscleLabel muscleLabel, LocalDate from, LocalDate to);
}
