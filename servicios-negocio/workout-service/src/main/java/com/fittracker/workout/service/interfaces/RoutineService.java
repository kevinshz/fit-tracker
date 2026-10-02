package com.fittracker.workout.service.interfaces;

import com.fittracker.workout.presentation.dto.RoutineRequest;
import com.fittracker.workout.presentation.dto.RoutineResponse;

import java.util.List;
import java.util.UUID;

public interface RoutineService {

    RoutineResponse createRoutine(RoutineRequest request, UUID userId);

    List<RoutineResponse> listRoutines(UUID userId);

    RoutineResponse getRoutine(UUID routineId, UUID userId);

    RoutineResponse updateRoutine(UUID routineId, RoutineRequest request, UUID userId);

    void deleteRoutine(UUID routineId, UUID userId);
}
