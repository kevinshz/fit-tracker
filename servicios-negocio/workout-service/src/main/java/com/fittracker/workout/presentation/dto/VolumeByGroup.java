package com.fittracker.workout.presentation.dto;

import java.math.BigDecimal;

public record VolumeByGroup(
        String groupName,
        BigDecimal totalVolumeKg,
        int totalSets
) {
}
