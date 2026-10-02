package com.fittracker.routine.persistence.repository;

import com.fittracker.routine.persistence.entity.ExerciseAlias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExerciseAliasRepository extends JpaRepository<ExerciseAlias, UUID> {

    boolean existsByAliasIgnoreCase(String alias);
}
