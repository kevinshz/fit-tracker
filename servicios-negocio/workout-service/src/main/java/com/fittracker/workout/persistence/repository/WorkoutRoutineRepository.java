package com.fittracker.workout.persistence.repository;

import com.fittracker.workout.persistence.entity.WorkoutRoutine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkoutRoutineRepository extends JpaRepository<WorkoutRoutine, UUID> {

    List<WorkoutRoutine> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
