package com.fittracker.workout.service.interfaces;

import com.fittracker.workout.presentation.dto.AddSessionExerciseRequest;
import com.fittracker.workout.presentation.dto.LogSetRequest;
import com.fittracker.workout.presentation.dto.SessionExerciseDto;
import com.fittracker.workout.presentation.dto.SessionResponse;
import com.fittracker.workout.presentation.dto.SetResponse;
import com.fittracker.workout.presentation.dto.StartSessionRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface WorkoutSessionService {

    SessionResponse startSession(StartSessionRequest request, UUID userId);

    SetResponse logSet(UUID sessionId, LogSetRequest request);

    SessionResponse getSession(UUID sessionId);

    void deleteSession(UUID sessionId);

    List<SessionResponse> getSessions(UUID userId, LocalDate from, LocalDate to);

    SessionExerciseDto addSessionExercise(UUID sessionId, AddSessionExerciseRequest request, UUID userId);

    void removeSessionExercise(UUID sessionId, UUID exerciseId, UUID userId);
}
