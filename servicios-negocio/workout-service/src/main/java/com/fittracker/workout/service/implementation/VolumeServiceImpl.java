package com.fittracker.workout.service.implementation;

import com.fittracker.workout.persistence.entity.WorkoutSession;
import com.fittracker.workout.persistence.entity.WorkoutSet;
import com.fittracker.workout.persistence.entity.enums.MuscleLabel;
import com.fittracker.workout.persistence.repository.WorkoutSessionRepository;
import com.fittracker.workout.persistence.repository.WorkoutSetRepository;
import com.fittracker.workout.presentation.dto.VolumeByGroup;
import com.fittracker.workout.presentation.dto.VolumeReportDto;
import com.fittracker.workout.service.exception.SessionNotFoundException;
import com.fittracker.workout.service.interfaces.VolumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VolumeServiceImpl implements VolumeService {

    private final WorkoutSessionRepository sessionRepository;
    private final WorkoutSetRepository setRepository;

    @Override
    @Transactional(readOnly = true)
    public VolumeReportDto getSessionVolume(UUID sessionId) {
        WorkoutSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        List<WorkoutSet> sets = setRepository.findBySessionId(sessionId);

        BigDecimal totalVolume = sets.stream()
                .map(WorkoutSet::getVolumeKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        VolumeByGroup group = new VolumeByGroup(
                session.getMuscleLabel().name(),
                totalVolume,
                sets.size()
        );

        return new VolumeReportDto(
                session.getPerformedAt(),
                session.getPerformedAt(),
                List.of(group)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public VolumeReportDto getUserVolume(UUID userId, LocalDate from, LocalDate to, String groupBy) {
        List<VolumeByGroup> items;

        if ("exercise".equalsIgnoreCase(groupBy)) {
            items = aggregateByExercise(userId, from, to);
        } else {
            items = aggregateByMuscleGroup(userId, from, to);
        }

        return new VolumeReportDto(from, to, items);
    }

    private List<VolumeByGroup> aggregateByExercise(UUID userId, LocalDate from, LocalDate to) {
        List<Object[]> results = setRepository.aggregateVolumeByExercise(userId, from, to);
        List<VolumeByGroup> items = new ArrayList<>();

        for (Object[] row : results) {
            UUID exerciseId = (UUID) row[0];
            String exerciseName = (String) row[1];
            BigDecimal totalVolume = (BigDecimal) row[2];
            Long totalSets = (Long) row[3];

            items.add(new VolumeByGroup(
                    exerciseName + " (" + exerciseId + ")",
                    totalVolume,
                    totalSets.intValue()
            ));
        }

        return items;
    }

    private List<VolumeByGroup> aggregateByMuscleGroup(UUID userId, LocalDate from, LocalDate to) {
        List<Object[]> results = setRepository.aggregateVolumeByMuscleGroup(userId, from, to);
        List<VolumeByGroup> items = new ArrayList<>();

        for (Object[] row : results) {
            MuscleLabel muscleLabel = (MuscleLabel) row[0];
            BigDecimal totalVolume = (BigDecimal) row[1];
            Long totalSets = (Long) row[2];

            items.add(new VolumeByGroup(
                    muscleLabel.name(),
                    totalVolume,
                    totalSets.intValue()
            ));
        }

        return items;
    }
}
