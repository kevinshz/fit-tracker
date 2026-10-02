package com.fittracker.user;

import com.fittracker.user.presentation.dto.CreateProfileRequest;
import com.fittracker.user.presentation.dto.UpdateProfileRequest;
import com.fittracker.user.presentation.dto.UserProfileResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@EnabledIfSystemProperty(named = "testcontainers.enabled", matches = "true",
        disabledReason = "Requiere Docker (usar -Dtestcontainers.enabled=true)")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("UserProfile - Flujo CRUD completo (Testcontainers)")
class UserProfileFlowIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private TestRestTemplate rest;

    private static final UUID USER_ID = UUID.randomUUID();

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("X-User-Id", USER_ID.toString());
        return h;
    }

    @Test
    @Order(1)
    @DisplayName("POST /users crea el perfil (201)")
    void createProfile() {
        var request = new CreateProfileRequest(USER_ID, "profile-" + UUID.randomUUID() + "@test.com",
                "Perfil Test", 75.5, 178.0, null, null, "fuerza", "intermedio");

        ResponseEntity<UserProfileResponse> response = rest.postForEntity(
                "/api/v1/users", new HttpEntity<>(request, headers()), UserProfileResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().userId()).isEqualTo(USER_ID);
        assertThat(response.getBody().bodyWeight()).isEqualTo(75.5);
    }

    @Test
    @Order(2)
    @DisplayName("Perfil duplicado retorna 409")
    void createProfile_duplicate_returns409() {
        var request = new CreateProfileRequest(USER_ID, "dup@test.com", "Dup", null, null, null, null, null, null);

        ResponseEntity<Map> response = rest.postForEntity(
                "/api/v1/users", new HttpEntity<>(request, headers()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @Order(3)
    @DisplayName("GET /users/me retorna el perfil")
    void getMyProfile() {
        ResponseEntity<UserProfileResponse> response = rest.exchange(
                "/api/v1/users/me", HttpMethod.GET, new HttpEntity<>(headers()), UserProfileResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Perfil Test");
    }

    @Test
    @Order(4)
    @DisplayName("PUT /users/me actualiza campos parciales")
    void updateProfile() {
        var request = new UpdateProfileRequest("Nombre Nuevo", 77.0, null, null, null, null, null);

        ResponseEntity<UserProfileResponse> response = rest.exchange(
                "/api/v1/users/me", HttpMethod.PUT, new HttpEntity<>(request, headers()),
                UserProfileResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Nombre Nuevo");
        assertThat(response.getBody().bodyWeight()).isEqualTo(77.0);
        assertThat(response.getBody().height()).isEqualTo(178.0);
    }

    @Test
    @Order(5)
    @DisplayName("DELETE /users/me elimina el perfil (204)")
    void deleteProfile() {
        ResponseEntity<Void> response = rest.exchange(
                "/api/v1/users/me", HttpMethod.DELETE, new HttpEntity<>(headers()), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @Order(6)
    @DisplayName("GET /users/me tras eliminar retorna 404")
    void getMyProfile_afterDelete_returns404() {
        ResponseEntity<Map> response = rest.exchange(
                "/api/v1/users/me", HttpMethod.GET, new HttpEntity<>(headers()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(7)
    @DisplayName("GET /users/me sin perfil inexistente para otro usuario retorna 404")
    void getProfile_otherUser_returns404() {
        HttpHeaders h = headers();
        h.set("X-User-Id", UUID.randomUUID().toString());

        ResponseEntity<Map> response = rest.exchange(
                "/api/v1/users/me", HttpMethod.GET, new HttpEntity<>(h), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
