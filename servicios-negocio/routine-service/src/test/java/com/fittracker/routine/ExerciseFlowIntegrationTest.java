package com.fittracker.routine;

import com.fittracker.routine.presentation.dto.ExerciseRequest;
import com.fittracker.routine.presentation.dto.ExerciseResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@EnabledIfSystemProperty(named = "testcontainers.enabled", matches = "true",
        disabledReason = "Requiere Docker (usar -Dtestcontainers.enabled=true)")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Exercise - Flujo CRUD completo (Testcontainers)")
class ExerciseFlowIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private TestRestTemplate rest;

    private static UUID exerciseId;
    private static final String UNIQUE_NAME = "Ejercicio Custom " + UUID.randomUUID();

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("X-User-Id", "user-flow-test");
        return h;
    }

    @Test
    @Order(1)
    @DisplayName("POST /exercises crea ejercicio custom (201)")
    void createExercise() {
        var request = new ExerciseRequest(UNIQUE_NAME, com.fittracker.routine.persistence.entity.enums.ExerciseType.STRENGTH,
                com.fittracker.routine.persistence.entity.enums.MuscleGroup.CHEST,
                List.of(com.fittracker.routine.persistence.entity.enums.MuscleGroup.TRICEPS),
                com.fittracker.routine.persistence.entity.enums.Equipment.BARBELL,
                null, List.of("Alias " + UUID.randomUUID()));

        ResponseEntity<ExerciseResponse> response = rest.postForEntity(
                "/api/v1/exercises", new HttpEntity<>(request, headers()), ExerciseResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo(UNIQUE_NAME);
        assertThat(response.getBody().isCustom()).isTrue();
        assertThat(response.getBody().createdBy()).isEqualTo("user-flow-test");
        exerciseId = response.getBody().id();
    }

    @Test
    @Order(2)
    @DisplayName("GET /exercises/{id} retorna el ejercicio")
    void getExercise() {
        ResponseEntity<ExerciseResponse> response = rest.exchange(
                "/api/v1/exercises/" + exerciseId, HttpMethod.GET,
                new HttpEntity<>(headers()), ExerciseResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().aliases()).hasSize(1);
    }

    @Test
    @Order(3)
    @DisplayName("GET /exercises/search?q= encuentra por nombre")
    void searchExercise() {
        ResponseEntity<Map<String, Object>> response = rest.exchange(
                "/api/v1/exercises/search?q=" + UNIQUE_NAME.substring(0, 20),
                HttpMethod.GET, new HttpEntity<>(headers()),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        @SuppressWarnings("unchecked")
        var content = (List<Map<String, Object>>) response.getBody().get("content");
        assertThat(content).extracting(m -> m.get("name")).contains(UNIQUE_NAME);
    }

    @Test
    @Order(4)
    @DisplayName("PUT /exercises/{id} actualiza el ejercicio custom")
    void updateExercise() {
        var request = new ExerciseRequest(UNIQUE_NAME + " v2",
                com.fittracker.routine.persistence.entity.enums.ExerciseType.STRENGTH,
                com.fittracker.routine.persistence.entity.enums.MuscleGroup.CHEST,
                null, com.fittracker.routine.persistence.entity.enums.Equipment.DUMBBELL,
                null, null);

        ResponseEntity<ExerciseResponse> response = rest.exchange(
                "/api/v1/exercises/" + exerciseId, HttpMethod.PUT,
                new HttpEntity<>(request, headers()), ExerciseResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo(UNIQUE_NAME + " v2");
        assertThat(response.getBody().equipment()).isEqualTo(com.fittracker.routine.persistence.entity.enums.Equipment.DUMBBELL);
    }

    @Test
    @Order(5)
    @DisplayName("POST /exercises nombre duplicado retorna error 4xx")
    void createDuplicate_returnsError() {
        var request = new ExerciseRequest(UNIQUE_NAME + " v2",
                com.fittracker.routine.persistence.entity.enums.ExerciseType.STRENGTH,
                com.fittracker.routine.persistence.entity.enums.MuscleGroup.CHEST,
                null, com.fittracker.routine.persistence.entity.enums.Equipment.BARBELL,
                null, null);

        ResponseEntity<Map> response = rest.postForEntity(
                "/api/v1/exercises", new HttpEntity<>(request, headers()), Map.class);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    @Order(6)
    @DisplayName("DELETE /exercises/{id} elimina (204) y despues 404")
    void deleteExercise() {
        ResponseEntity<Void> delete = rest.exchange(
                "/api/v1/exercises/" + exerciseId, HttpMethod.DELETE,
                new HttpEntity<>(headers()), Void.class);
        assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Map> get = rest.exchange(
                "/api/v1/exercises/" + exerciseId, HttpMethod.GET,
                new HttpEntity<>(headers()), Map.class);
        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(7)
    @DisplayName("GET /exercises/catalogo base no se puede borrar (error)")
    void baseCatalogDelete_fails() {
        ResponseEntity<Map> list = rest.exchange(
                "/api/v1/exercises?page=0&size=1", HttpMethod.GET,
                new HttpEntity<>(headers()),
                new ParameterizedTypeReference<>() {});
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        var content = (List<Map<String, Object>>) list.getBody().get("content");
        assertThat(content).isNotEmpty();
        UUID baseId = UUID.fromString((String) content.get(0).get("id"));

        ResponseEntity<Map> response = rest.exchange(
                "/api/v1/exercises/" + baseId, HttpMethod.DELETE,
                new HttpEntity<>(headers()), Map.class);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }
}
