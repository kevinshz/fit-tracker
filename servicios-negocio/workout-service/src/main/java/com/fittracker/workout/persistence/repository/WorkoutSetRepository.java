package com.fittracker.workout.persistence.repository;

import com.fittracker.workout.persistence.entity.WorkoutSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorkoutSetRepository extends JpaRepository<WorkoutSet, UUID> {

    List<WorkoutSet> findBySessionId(UUID sessionId);

    @Query("SELECT ws.exerciseId AS exerciseId, ws.exerciseName AS exerciseName, " +
            "SUM(ws.volumeKg) AS totalVolume, COUNT(ws) AS totalSets " +
            "FROM WorkoutSet ws " +
            "JOIN ws.session s " +
            "WHERE s.userId = :userId AND s.performedAt BETWEEN :from AND :to " +
            "GROUP BY ws.exerciseId, ws.exerciseName " +
            "ORDER BY totalVolume DESC")
    List<Object[]> aggregateVolumeByExercise(@Param("userId") UUID userId,
                                              @Param("from") java.time.LocalDate from,
                                              @Param("to") java.time.LocalDate to);

    @Query("SELECT s.muscleLabel AS muscleLabel, SUM(ws.volumeKg) AS totalVolume, COUNT(ws) AS totalSets " +
            "FROM WorkoutSet ws " +
            "JOIN ws.session s " +
            "WHERE s.userId = :userId AND s.performedAt BETWEEN :from AND :to " +
            "GROUP BY s.muscleLabel " +
            "ORDER BY totalVolume DESC")
    List<Object[]> aggregateVolumeByMuscleGroup(@Param("userId") UUID userId,
                                                 @Param("from") java.time.LocalDate from,
                                                 @Param("to") java.time.LocalDate to);

    @Query("SELECT SUM(ws.volumeKg) FROM WorkoutSet ws WHERE ws.session.userId = :userId")
    BigDecimal sumVolumeByUserId(@Param("userId") UUID userId);

    @Query("SELECT SUM(ws.volumeKg) FROM WorkoutSet ws WHERE ws.exerciseId = :exerciseId AND ws.session.userId = :userId")
    BigDecimal sumVolumeByExerciseAndUser(@Param("exerciseId") UUID exerciseId, @Param("userId") UUID userId);

    @Query("SELECT ws FROM WorkoutSet ws JOIN FETCH ws.session s " +
            "WHERE ws.exerciseId = :exerciseId AND s.userId = :userId " +
            "ORDER BY s.performedAt DESC, ws.setNumber ASC")
    List<WorkoutSet> findByExerciseIdAndSessionUserId(@Param("exerciseId") UUID exerciseId,
                                                       @Param("userId") UUID userId);
}
