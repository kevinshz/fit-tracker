package com.fittracker.workout.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workout_set", indexes = {
        @Index(name = "idx_wset_session", columnList = "session_id"),
        @Index(name = "idx_wset_exercise", columnList = "exercise_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutSet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private WorkoutSession session;

    @Column(name = "exercise_id", nullable = false)
    private UUID exerciseId;

    @Column(name = "exercise_name", nullable = false, length = 200)
    private String exerciseName;

    @Column(name = "set_number", nullable = false)
    private int setNumber;

    @Column(name = "weight_kg", nullable = false, precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Column(nullable = false)
    private int reps;

    @Column(name = "rir")
    private Integer rir;

    @Column(name = "volume_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal volumeKg;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (volumeKg == null && weightKg != null) {
            volumeKg = weightKg.multiply(BigDecimal.valueOf(reps));
        }
    }
}
