package com.fittracker.auth;

import com.fittracker.auth.presentation.dto.AuthResponse;
import com.fittracker.auth.presentation.dto.LoginRequest;
import com.fittracker.auth.presentation.dto.RefreshTokenRequest;
import com.fittracker.auth.presentation.dto.RegisterRequest;
import com.fittracker.auth.presentation.dto.TokenValidationRequest;
import com.fittracker.auth.presentation.dto.TokenValidationResponse;
import com.fittracker.auth.service.messaging.UserEventProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;

@Testcontainers
@EnabledIfSystemProperty(named = "testcontainers.enabled", matches = "true",
        disabledReason = "Requiere Docker (usar -Dtestcontainers.enabled=true)")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Auth - Flujo completo de integracion (Testcontainers)")
class AuthFlowIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private TestRestTemplate rest;

    @MockBean private UserEventProducer eventProducer;

    private static String token;
    private static String email;

    @Test
    @Order(1)
    @DisplayName("POST /auth/register crea usuario y retorna JWT")
    void register_createsUser() {
        email = "flow-" + UUID.randomUUID() + "@test.com";
        var request = new RegisterRequest(email, "Secret123!", "Flow Test");

        ResponseEntity<AuthResponse> response = rest.postForEntity("/auth/register", request, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isNotBlank();
        assertThat(response.getBody().email()).isEqualTo(email);
        token = response.getBody().token();

        verify(eventProducer).sendUserCreatedEvent(anyMap());
    }

    @Test
    @Order(2)
    @DisplayName("POST /auth/register email duplicado retorna 409")
    void register_duplicate_returns409() {
        var request = new RegisterRequest(email, "Secret123!", "Flow Test");

        ResponseEntity<Map> response = rest.postForEntity("/auth/register", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @Order(3)
    @DisplayName("POST /auth/login con credenciales correctas retorna JWT")
    void login_success() {
        var request = new LoginRequest(email, "Secret123!");

        ResponseEntity<AuthResponse> response = rest.postForEntity("/auth/login", request, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isNotBlank();
        assertThat(response.getBody().userId()).isNotNull();
    }

    @Test
    @Order(4)
    @DisplayName("POST /auth/login con password incorrecto retorna 401")
    void login_wrongPassword_returns401() {
        var request = new LoginRequest(email, "WrongPassword");

        ResponseEntity<Map> response = rest.postForEntity("/auth/login", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(5)
    @DisplayName("POST /auth/validate-token valida el JWT emitido")
    void validateToken_valid() {
        var request = new TokenValidationRequest(token);

        ResponseEntity<TokenValidationResponse> response =
                rest.postForEntity("/auth/validate-token", request, TokenValidationResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().valid()).isTrue();
        assertThat(response.getBody().email()).isEqualTo(email);
        assertThat(response.getBody().roles()).contains("ROLE_USER");
    }

    @Test
    @Order(6)
    @DisplayName("POST /auth/validate-token con token corrupto retorna valid=false")
    void validateToken_corrupt_invalid() {
        var request = new TokenValidationRequest("token-corrupto");

        ResponseEntity<TokenValidationResponse> response =
                rest.postForEntity("/auth/validate-token", request, TokenValidationResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().valid()).isFalse();
    }

    @Test
    @Order(7)
    @DisplayName("POST /auth/refresh-token emite un JWT nuevo")
    void refreshToken_success() {
        var request = new RefreshTokenRequest(token);

        ResponseEntity<AuthResponse> response = rest.postForEntity("/auth/refresh-token", request, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isNotBlank();
        assertThat(response.getBody().email()).isEqualTo(email);
    }

    @Test
    @Order(8)
    @DisplayName("POST /auth/register sin password retorna 400")
    void register_withoutPassword_returns400() {
        var request = new RegisterRequest("nopass-" + UUID.randomUUID() + "@test.com", "", "Test");

        ResponseEntity<Map> response = rest.postForEntity("/auth/register", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
