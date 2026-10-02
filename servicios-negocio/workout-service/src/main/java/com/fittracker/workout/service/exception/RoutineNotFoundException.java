package com.fittracker.workout.service.exception;

public class RoutineNotFoundException extends RuntimeException {

    public RoutineNotFoundException(String message) {
        super(message);
    }

    public RoutineNotFoundException(java.util.UUID routineId) {
        super("Rutina no encontrada: " + routineId);
    }
}
