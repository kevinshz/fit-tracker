package com.fittracker.workout.service.exception;

public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(String message) {
        super(message);
    }

    public SessionNotFoundException(java.util.UUID sessionId) {
        super("Sesión de entrenamiento no encontrada: " + sessionId);
    }
}
