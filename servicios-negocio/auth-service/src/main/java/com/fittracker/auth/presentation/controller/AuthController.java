package com.fittracker.auth.presentation.controller;

import com.fittracker.auth.presentation.dto.*;
import com.fittracker.auth.service.interfaces.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registro, login y gestion de tokens JWT")
public class AuthController {

    private final AuthService authService;

    @SecurityRequirements
    @Operation(summary = "Registrar un usuario nuevo",
            description = "Crea el usuario y emite un JWT. Devuelve 409 si el email ya existe.")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @SecurityRequirements
    @Operation(summary = "Iniciar sesion",
            description = "Autentica con email y clave. Devuelve 401 si las credenciales son incorrectas.")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @SecurityRequirements
    @Operation(summary = "Validar un token JWT",
            description = "Comprueba firma y expiracion sin autenticar al usuario.")
    @PostMapping("/validate-token")
    public ResponseEntity<TokenValidationResponse> validateToken(
            @RequestBody TokenValidationRequest request) {
        TokenValidationResponse response = authService.validateToken(request.token());
        return ResponseEntity.ok(response);
    }

    @SecurityRequirements
    @Operation(summary = "Refrescar un token JWT",
            description = "Emite un token nuevo a partir de uno existente. Devuelve 401 si es invalido o expirado.")
    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(
            @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request.token());
        return ResponseEntity.ok(response);
    }
}
