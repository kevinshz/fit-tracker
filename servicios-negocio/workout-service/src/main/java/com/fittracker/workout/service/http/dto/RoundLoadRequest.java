package com.fittracker.workout.service.http.dto;

import java.math.BigDecimal;

public record RoundLoadRequest(BigDecimal targetWeight, String equipment) {
}
