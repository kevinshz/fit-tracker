package com.fittracker.auth.presentation.advice;

import com.fittracker.auth.service.exception.EmailAlreadyExistsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class AuthExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleEmailAlreadyExists(
            EmailAlreadyExistsException ex) {
        log.warn("Email ya registrado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "timestamp", LocalDateTime.now().toString(),
                        "status", 409,
                        "error", "Conflict",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException ex) {
        log.warn("Credenciales inválidas: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "timestamp", LocalDateTime.now().toString(),
                        "status", 401,
                        "error", "Unauthorized",
                        "message", "Credenciales inválidas"
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Error de validación");
        log.warn("Error de validación: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "timestamp", LocalDateTime.now().toString(),
                        "status", 400,
                        "error", "Bad Request",
                        "message", message
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Error inesperado: ", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "timestamp", LocalDateTime.now().toString(),
                        "status", 400,
                        "error", "Bad Request",
                        "message", ex.getMessage() != null ? ex.getMessage() : "Error inesperado"
                ));
    }
}
