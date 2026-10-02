package com.fittracker.workout.persistence.repository;

import com.fittracker.workout.persistence.entity.SessionExercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionExerciseRepository extends JpaRepository<SessionExercise, UUID> {

    Optional<SessionExercise> findBySessionIdAndExerciseId(UUID sessionId, UUID exerciseId);
}
