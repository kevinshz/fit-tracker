package com.fittracker.workout.service.http.dto;

import java.math.BigDecimal;

public record OneRepMaxRequest(BigDecimal weight, Integer reps) {
}
