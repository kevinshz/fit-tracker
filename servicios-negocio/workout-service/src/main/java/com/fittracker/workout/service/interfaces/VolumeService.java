package com.fittracker.workout.service.interfaces;

import com.fittracker.workout.presentation.dto.VolumeReportDto;

import java.time.LocalDate;
import java.util.UUID;

public interface VolumeService {

    VolumeReportDto getSessionVolume(UUID sessionId);

    VolumeReportDto getUserVolume(UUID userId, LocalDate from, LocalDate to, String groupBy);
}
