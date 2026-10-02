package com.fittracker.common.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.MapBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler - Tests Unitarios")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("ResourceNotFoundException retorna 404 con mensaje")
    void resourceNotFound_returns404() {
        var response = handler.handleResourceNotFound(new ResourceNotFoundException("Ejercicio no encontrado"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).containsEntry("message", "Ejercicio no encontrado");
    }

    @Test
    @DisplayName("IllegalStateException retorna 409 con mensaje")
    void illegalState_returns409() {
        var response = handler.handleIllegalState(new IllegalStateException("Email ya registrado"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).containsEntry("message", "Email ya registrado");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException retorna 400 con errores por campo")
    void validation_returns400_withFieldErrors() throws Exception {
        BindingResult bindingResult = new MapBindingResult(new java.util.HashMap<>(), "request");
        bindingResult.rejectValue("email", "NotBlank", "el email es obligatorio");
        bindingResult.rejectValue("password", "Size", "la contraseña debe tener al menos 8 caracteres");

        Method method = Object.class.getMethod("hashCode");
        MethodParameter parameter = new MethodParameter(method, -1);
        var exception = new MethodArgumentNotValidException(parameter, bindingResult);

        var response = handler.handleValidation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error())
                .containsEntry("email", "el email es obligatorio")
                .containsEntry("password", "la contraseña debe tener al menos 8 caracteres");
    }

    @Test
    @DisplayName("Exception generica retorna 400 con mensaje generico")
    void genericException_returns400_withGenericMessage() {
        var response = handler.handleException(new RuntimeException("boom"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().get("message"))
                .contains("se ha producido un error");
    }

    @Test
    @DisplayName("ErrorResponse es un record con mapa de errores")
    void errorResponse_isRecord() {
        ErrorResponse response = new ErrorResponse(java.util.Map.of("key", "value"));
        assertThat(response.error()).containsEntry("key", "value");
    }
}
