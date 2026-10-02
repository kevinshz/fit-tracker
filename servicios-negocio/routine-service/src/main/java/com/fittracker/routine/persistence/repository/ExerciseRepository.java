package com.fittracker.routine.persistence.repository;

import com.fittracker.routine.persistence.entity.Exercise;
import com.fittracker.routine.persistence.entity.enums.Equipment;
import com.fittracker.routine.persistence.entity.enums.MuscleGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID>, JpaSpecificationExecutor<Exercise> {

    boolean existsByExternalId(String externalId);

    boolean existsByNameIgnoreCase(String name);

    Optional<Exercise> findByNameIgnoreCase(String name);

    Optional<Exercise> findByExternalId(String externalId);

    Page<Exercise> findByPrimaryMuscle(MuscleGroup muscle, Pageable pageable);

    Page<Exercise> findByEquipment(Equipment equipment, Pageable pageable);

    @Query("""
            select distinct e from Exercise e
            left join e.aliases a
            where lower(e.name) like lower(concat('%', :q, '%'))
               or lower(a.alias) like lower(concat('%', :q, '%'))
            """)
    Page<Exercise> searchByNameOrAlias(@Param("q") String query, Pageable pageable);
}
