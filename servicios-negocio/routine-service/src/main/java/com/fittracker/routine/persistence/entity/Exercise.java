package com.fittracker.routine.persistence.entity;

import com.fittracker.routine.persistence.entity.enums.Equipment;
import com.fittracker.routine.persistence.entity.enums.ExerciseType;
import com.fittracker.routine.persistence.entity.enums.MuscleGroup;
import com.fittracker.routine.persistence.entity.enums.ProgressionStrategy;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "exercise",
        uniqueConstraints = @UniqueConstraint(name = "uk_exercise_name", columnNames = "name"),
        indexes = {
                @Index(name = "idx_exercise_external_id", columnList = "externalId"),
                @Index(name = "idx_exercise_primary_muscle", columnList = "primaryMuscle"),
                @Index(name = "idx_exercise_equipment", columnList = "equipment")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "external_id")
    private String externalId;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExerciseType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_muscle", nullable = false, length = 30)
    private MuscleGroup primaryMuscle;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "exercise_secondary_muscles", joinColumns = @JoinColumn(name = "exercise_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "muscle", length = 30)
    @Builder.Default
    private List<MuscleGroup> secondaryMuscles = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Equipment equipment;

    @Enumerated(EnumType.STRING)
    @Column(name = "progression_strategy", nullable = false, length = 30)
    private ProgressionStrategy progressionStrategy;

    @Column(nullable = false)
    private boolean isCustom;

    private String createdBy;

    @Lob
    @Column(name = "instructions_es", columnDefinition = "text")
    private String instructionsEs;

    @Lob
    @Column(name = "instructions_en", columnDefinition = "text")
    private String instructionsEn;

    @Column(name = "image_url", length = 300)
    private String imageUrl;

    @Column(name = "gif_url", length = 300)
    private String gifUrl;

    @Column(length = 500)
    private String attribution;

    @OneToMany(mappedBy = "exercise", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExerciseAlias> aliases = new ArrayList<>();
}
