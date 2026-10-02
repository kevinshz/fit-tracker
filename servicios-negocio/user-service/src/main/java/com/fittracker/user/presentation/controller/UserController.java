package com.fittracker.user.presentation.controller;

import com.fittracker.user.presentation.dto.CreateProfileRequest;
import com.fittracker.user.presentation.dto.UpdateProfileRequest;
import com.fittracker.user.presentation.dto.UserProfileResponse;
import com.fittracker.user.service.interfaces.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Perfiles de usuario: consulta, actualizacion y eliminacion")
public class UserController {

    private final UserProfileService userProfileService;

    @Operation(summary = "Obtener mi perfil",
            description = "Usa el header X-User-Id inyectado por el gateway a partir del JWT.")
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getProfile(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(userProfileService.getProfileByUserId(userId));
    }

    @Operation(summary = "Actualizar mi perfil",
            description = "Actualiza peso, altura, objetivo y nivel de experiencia del perfil del usuario autenticado.")
    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userProfileService.updateProfile(userId, request));
    }

    @Operation(summary = "Crear un perfil (uso interno)",
            description = "Crea un perfil para un userId concreto. En el flujo normal el perfil se crea via evento RabbitMQ user.created.")
    @PostMapping
    public ResponseEntity<UserProfileResponse> createProfile(
            @Valid @RequestBody CreateProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userProfileService.createProfile(request));
    }

    @Operation(summary = "Obtener un perfil por id de usuario")
    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponse> getProfileById(@PathVariable UUID id) {
        return ResponseEntity.ok(userProfileService.getProfileByUserId(id));
    }

    @Operation(summary = "Eliminar mi perfil",
            description = "Elimina el perfil del usuario autenticado. Devuelve 204 sin contenido.")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteProfile(
            @RequestHeader("X-User-Id") UUID userId) {
        userProfileService.deleteProfile(userId);
        return ResponseEntity.noContent().build();
    }
}
