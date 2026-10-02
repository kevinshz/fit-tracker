package com.fittracker.workout;

import com.fittracker.workout.persistence.entity.enums.MuscleLabel;
import com.fittracker.workout.persistence.entity.enums.SessionType;
import com.fittracker.workout.presentation.dto.LogSetRequest;
import com.fittracker.workout.presentation.dto.SessionResponse;
import com.fittracker.workout.presentation.dto.SetResponse;
import com.fittracker.workout.presentation.dto.StartSessionRequest;
import com.fittracker.workout.service.messaging.SessionEventProducer;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@Testcontainers
@EnabledIfSystemProperty(named = "testcontainers.enabled", matches = "true",
        disabledReason = "Requiere Docker (usar -Dtestcontainers.enabled=true)")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Workout - Flujo completo de sesion (Testcontainers)")
class WorkoutSessionFlowIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private TestRestTemplate rest;

    @MockBean private SessionEventProducer eventProducer;

    private static final UUID USER_ID = UUID.randomUUID();
    private static UUID sessionId;

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("X-User-Id", USER_ID.toString());
        return h;
    }

    @Test
    @Order(1)
    @DisplayName("POST /sessions crea sesion con tipo explicito")
    void startSession() {
        var request = new StartSessionRequest(LocalDate.now(), MuscleLabel.CHEST_BACK,
                SessionType.HYPERTROPHY, "flujo de prueba", null);

        ResponseEntity<SessionResponse> response = rest.postForEntity(
                "/api/v1/workouts/sessions", new HttpEntity<>(request, headers()), SessionResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().sessionType()).isEqualTo(SessionType.HYPERTROPHY);
        assertThat(response.getBody().muscleLabel()).isEqualTo(MuscleLabel.CHEST_BACK);
        assertThat(response.getBody().sets()).isEmpty();
        sessionId = response.getBody().id();
    }

    @Test
    @Order(2)
    @DisplayName("POST /sessions/{id}/sets registra series y calcula volumen")
    void logSets() {
        var set1 = new LogSetRequest(UUID.randomUUID(), "Press banca", 1, new BigDecimal("60"), 8, 2);
        var set2 = new LogSetRequest(UUID.randomUUID(), "Press banca", 2, new BigDecimal("60"), 7, 1);

        ResponseEntity<SetResponse> r1 = rest.postForEntity(
                "/api/v1/workouts/sessions/" + sessionId + "/sets",
                new HttpEntity<>(set1, headers()), SetResponse.class);
        ResponseEntity<SetResponse> r2 = rest.postForEntity(
                "/api/v1/workouts/sessions/" + sessionId + "/sets",
                new HttpEntity<>(set2, headers()), SetResponse.class);

        assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(r1.getBody()).isNotNull();
        assertThat(r1.getBody().volumeKg()).isEqualByComparingTo("480");
        assertThat(r2.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(r2.getBody()).isNotNull();
        assertThat(r2.getBody().volumeKg()).isEqualByComparingTo("420");
    }

    @Test
    @Order(3)
    @DisplayName("POST /sets con peso invalido retorna error")
    void logSet_invalidWeight_fails() {
        var invalid = new LogSetRequest(UUID.randomUUID(), "Press banca", 3, BigDecimal.ZERO, 8, 2);

        ResponseEntity<Map> response = rest.postForEntity(
                "/api/v1/workouts/sessions/" + sessionId + "/sets",
                new HttpEntity<>(invalid, headers()), Map.class);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    @Order(4)
    @DisplayName("GET /sessions/{id} retorna la sesion con sus 2 sets")
    void getSession() {
        ResponseEntity<SessionResponse> response = rest.exchange(
                "/api/v1/workouts/sessions/" + sessionId, HttpMethod.GET,
                new HttpEntity<>(headers()), SessionResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().sets()).hasSize(2);
        assertThat(response.getBody().sets().get(0).setNumber()).isEqualTo(1);
    }

    @Test
    @Order(5)
    @DisplayName("GET /sessions lista sesiones del usuario")
    void listSessions() {
        ResponseEntity<SessionResponse[]> response = rest.exchange(
                "/api/v1/workouts/sessions", HttpMethod.GET,
                new HttpEntity<>(headers()), SessionResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    @Order(6)
    @DisplayName("GET /sessions?from&to filtra por rango de fechas")
    void listSessionsByDateRange() {
        ResponseEntity<SessionResponse[]> response = rest.exchange(
                "/api/v1/workouts/sessions?from=" + LocalDate.now().minusDays(1)
                        + "&to=" + LocalDate.now(),
                HttpMethod.GET, new HttpEntity<>(headers()), SessionResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    @Order(7)
    @DisplayName("DELETE /sessions/{id} emite evento y elimina (204)")
    void deleteSession() {
        ResponseEntity<Void> response = rest.exchange(
                "/api/v1/workouts/sessions/" + sessionId, HttpMethod.DELETE,
                new HttpEntity<>(headers()), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(eventProducer).sendSessionCompletedEvent(
                eq(sessionId), eq(USER_ID), anyString(), anyString());
    }

    @Test
    @Order(8)
    @DisplayName("GET /sessions/{id} inexistente retorna 404")
    void getSession_notFound() {
        ResponseEntity<Map> response = rest.exchange(
                "/api/v1/workouts/sessions/" + UUID.randomUUID(), HttpMethod.GET,
                new HttpEntity<>(headers()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
