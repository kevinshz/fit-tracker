package com.fittracker.workout.presentation.dto;

import java.time.LocalDate;
import java.util.List;

public record VolumeReportDto(
        LocalDate from,
        LocalDate to,
        List<VolumeByGroup> items
) {
}
