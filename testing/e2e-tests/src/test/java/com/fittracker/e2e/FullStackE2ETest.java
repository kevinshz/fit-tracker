package com.fittracker.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas E2E del stack completo a traves del api-gateway (:8222).
 * Requiere el stack Docker levantado: docker compose up -d desde docker/.
 * Ejecucion: mvn test -pl testing/e2e-tests "-De2e.enabled=true"
 */
@EnabledIfSystemProperty(named = "e2e.enabled", matches = "true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E stack completo via api-gateway")
class FullStackE2ETest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static String baseUrl;
    private static String token;
    private static String userId;
    private static String exerciseId;
    private static String exerciseName;
    private static String sessionId;
    private static String routineId;

    @BeforeAll
    static void setup() {
        baseUrl = System.getProperty("e2e.base-url", "http://localhost:8222");
    }

    // ---------- helpers ----------

    private record Res(int status, JsonNode body) {
    }

    private static Res call(String method, String path, String bearerToken, String body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(30));
            if (bearerToken != null) {
                builder.header("Authorization", "Bearer " + bearerToken);
            }
            if (body != null) {
                builder.header("Content-Type", "application/json");
                builder.method(method, HttpRequest.BodyPublishers.ofString(body));
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }
            HttpResponse<String> response = CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode node = response.body() == null || response.body().isBlank()
                    ? JSON.nullNode()
                    : JSON.readTree(response.body());
            return new Res(response.statusCode(), node);
        } catch (Exception e) {
            throw new IllegalStateException("Fallo llamada " + method + " " + path + ": " + e.getMessage(), e);
        }
    }

    private static String uniqueEmail() {
        return "e2e." + UUID.randomUUID().toString().substring(0, 8) + "@fittracker.test";
    }

    // ---------- flujo ----------

    @Test
    @Order(1)
    @DisplayName("registro emite token y userId")
    void registro() {
        String email = uniqueEmail();
        ObjectNode payload = JSON.createObjectNode()
                .put("email", email)
                .put("password", "Passw0rd!")
                .put("name", "Usuario E2E");

        Res res = call("POST", "/auth/register", null, payload.toString());

        assertThat(res.status()).isEqualTo(201);
        assertThat(res.body().path("token").asText()).isNotBlank();
        assertThat(res.body().path("userId").asText()).isNotBlank();
        token = res.body().path("token").asText();
        userId = res.body().path("userId").asText();
    }

    @Test
    @Order(2)
    @DisplayName("registro duplicado responde 409")
    void registroDuplicado() {
        ObjectNode payload = JSON.createObjectNode()
                .put("email", uniqueEmail())
                .put("password", "Passw0rd!")
                .put("name", "Duplicado");
        call("POST", "/auth/register", null, payload.toString());

        Res res = call("POST", "/auth/register", null, payload.toString());
        assertThat(res.status()).isEqualTo(409);
    }

    @Test
    @Order(3)
    @DisplayName("login con credenciales correctas e incorrectas")
    void login() {
        String email = uniqueEmail();
        call("POST", "/auth/register", null, JSON.createObjectNode()
                .put("email", email)
                .put("password", "Passw0rd!")
                .put("name", "Login E2E").toString());

        Res correcto = call("POST", "/auth/login", null, JSON.createObjectNode()
                .put("email", email)
                .put("password", "Passw0rd!").toString());
        assertThat(correcto.status()).isEqualTo(200);
        assertThat(correcto.body().path("token").asText()).isNotBlank();

        Res incorrecto = call("POST", "/auth/login", null, JSON.createObjectNode()
                .put("email", email)
                .put("password", "mala-clave").toString());
        assertThat(incorrecto.status()).isEqualTo(401);
    }

    @Test
    @Order(4)
    @DisplayName("GET /users/me via gateway con X-User-Id tras evento RabbitMQ")
    void perfilViaEvento() throws InterruptedException {
        JsonNode me = null;
        int status = 0;
        // el perfil se crea asincronamente via user.created (RabbitMQ): reintentar
        for (int i = 0; i < 20; i++) {
            Res res = call("GET", "/api/v1/users/me", token, null);
            status = res.status();
            if (status == 200) {
                me = res.body();
                break;
            }
            Thread.sleep(1000);
        }
        assertThat(status).isEqualTo(200);
        assertThat(me.path("userId").asText()).isEqualTo(userId);
        assertThat(me.path("email").asText()).isNotBlank();
    }

    @Test
    @Order(5)
    @DisplayName("CRUD de ejercicios: crear valido y rechazar invalido")
    void ejercicios() {
        String nombreUnico = "Press banca E2E " + UUID.randomUUID().toString().substring(0, 8);
        Res creado = call("POST", "/api/v1/exercises", token, JSON.createObjectNode()
                .put("name", nombreUnico)
                .put("type", "STRENGTH")
                .put("primaryMuscle", "CHEST")
                .put("equipment", "BARBELL")
                .toString());
        assertThat(creado.status()).isBetween(200, 299);
        exerciseId = creado.body().path("id").asText();
        assertThat(exerciseId).isNotBlank();
        exerciseName = nombreUnico;

        Res invalido = call("POST", "/api/v1/exercises", token, JSON.createObjectNode()
                .put("type", "STRENGTH")
                .toString());
        assertThat(invalido.status()).isEqualTo(400);

        Res listado = call("GET", "/api/v1/exercises?muscle=CHEST&page=0&size=5", token, null);
        assertThat(listado.status()).isEqualTo(200);
        assertThat(listado.body().path("totalElements").asInt()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Order(6)
    @DisplayName("calculator one-rep-max (Epley 100x5 = 116.67)")
    void calculadora() {
        Res res = call("POST", "/api/v1/calculator/one-rep-max", token, JSON.createObjectNode()
                .put("weight", 100)
                .put("reps", 5)
                .toString());
        assertThat(res.status()).isEqualTo(200);
        assertThat(res.body().path("oneRepMax").asDouble()).isEqualTo(116.67, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    @Order(7)
    @DisplayName("sesion de entrenamiento: crear, registrar serie, ver detalle")
    void sesionYSeries() {
        Res sesion = call("POST", "/api/v1/workouts/sessions", token, JSON.createObjectNode()
                .put("performedAt", java.time.LocalDate.now().toString())
                .put("muscleLabel", "PUSH")
                .put("sessionType", "HYPERTROPHY")
                .toString());
        assertThat(sesion.status()).isBetween(200, 299);
        sessionId = sesion.body().path("id").asText();
        assertThat(sessionId).isNotBlank();

        Res serie = call("POST", "/api/v1/workouts/sessions/" + sessionId + "/sets", token, JSON.createObjectNode()
                .put("exerciseId", exerciseId)
                .put("exerciseName", exerciseName)
                .put("setNumber", 1)
                .put("weightKg", 80)
                .put("reps", 8)
                .put("rir", 2)
                .toString());
        assertThat(serie.status()).isBetween(200, 299);
        assertThat(serie.body().path("volumeKg").asDouble()).isEqualTo(640.0, org.assertj.core.data.Offset.offset(0.01));

        Res detalle = call("GET", "/api/v1/workouts/sessions/" + sessionId, token, null);
        assertThat(detalle.status()).isEqualTo(200);
        assertThat(detalle.body().path("sets").size()).isEqualTo(1);
    }

    @Test
    @Order(8)
    @DisplayName("reporte de volumen agrupado por musculo")
    void reporteVolumen() {
        Res res = call("GET", "/api/v1/workouts/volume/report?from=2000-01-01&to=2100-01-01", token, null);
        assertThat(res.status()).isEqualTo(200);
        assertThat(res.body().path("items").isArray()).isTrue();
        boolean conVolumen = false;
        for (JsonNode item : res.body().path("items")) {
            if (item.path("totalVolumeKg").asDouble() >= 640.0) {
                conVolumen = true;
            }
        }
        assertThat(conVolumen).as("debe existir un grupo con volumen >= 640 kg").isTrue();
    }

    @Test
    @Order(9)
    @DisplayName("busqueda de ejercicios por texto")
    void busqueda() {
        Res res = call("GET", "/api/v1/exercises/search?q=banca&size=5", token, null);
        assertThat(res.status()).isEqualTo(200);
        assertThat(res.body().path("totalElements").asInt()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Order(10)
    @DisplayName("actualizacion de perfil via PUT /users/me")
    void actualizarPerfil() {
        Res res = call("PUT", "/api/v1/users/me", token, JSON.createObjectNode()
                .put("bodyWeight", 74.2)
                .put("height", 178.5)
                .put("fitnessGoal", "MUSCLE_GAIN")
                .put("experienceLevel", "INTERMEDIATE")
                .toString());
        assertThat(res.status()).isEqualTo(200);
        assertThat(res.body().path("bodyWeight").asDouble()).isEqualTo(74.2, org.assertj.core.data.Offset.offset(0.01));
        assertThat(res.body().path("height").asDouble()).isEqualTo(178.5, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    @Order(11)
    @DisplayName("gateway rechaza token invalido y peticion sin token (401)")
    void seguridadGateway() {
        Res sinToken = call("GET", "/api/v1/users/me", null, null);
        Assertions.assertEquals(401, sinToken.status(), "sin token debe ser 401");

        Res tokenFalso = call("GET", "/api/v1/users/me", "token-basura-no-valido", null);
        Assertions.assertEquals(401, tokenFalso.status(), "token invalido debe ser 401");

        Res registro = call("GET", "/auth/me", "token-basura-no-valido", null);
        // /auth/** esta en skip-path: el gateway no valida, responde el servicio o 404/405
        assertThat(registro.status()).isNotEqualTo(500);
    }

    @Test
    @Order(12)
    @DisplayName("CRUD de rutinas: crear, listar, editar, validar y eliminar")
    void crudRutinas() {
        ObjectNode payload = JSON.createObjectNode()
                .put("name", "Rutina Push E2E")
                .put("sessionType", "STRENGTH")
                .put("muscleLabel", "PUSH")
                .put("notes", "Prueba e2e de rutinas");
        payload.putArray("exercises")
                .addObject()
                .put("exerciseId", exerciseId)
                .put("plannedSets", 4);

        Res creado = call("POST", "/api/v1/workouts/routines", token, payload.toString());
        assertThat(creado.status()).isEqualTo(201);
        routineId = creado.body().path("id").asText();
        assertThat(routineId).isNotBlank();
        assertThat(creado.body().path("exercises").size()).isEqualTo(1);
        assertThat(creado.body().path("exercises").get(0).path("plannedSets").asInt()).isEqualTo(4);
        assertThat(creado.body().path("exercises").get(0).path("exerciseName").asText()).isEqualTo(exerciseName);

        Res listado = call("GET", "/api/v1/workouts/routines", token, null);
        assertThat(listado.status()).isEqualTo(200);
        boolean encontrada = false;
        for (JsonNode rutina : listado.body()) {
            if (routineId.equals(rutina.path("id").asText())) {
                encontrada = true;
            }
        }
        assertThat(encontrada).as("la rutina creada debe aparecer en el listado").isTrue();

        ObjectNode editPayload = JSON.createObjectNode()
                .put("name", "Rutina Push E2E v2")
                .put("sessionType", "HYPERTROPHY")
                .put("muscleLabel", "PUSH")
                .putNull("notes");
        editPayload.putArray("exercises")
                .addObject()
                .put("exerciseId", exerciseId)
                .put("plannedSets", 5);
        Res editado = call("PUT", "/api/v1/workouts/routines/" + routineId, token, editPayload.toString());
        assertThat(editado.status()).isEqualTo(200);
        assertThat(editado.body().path("name").asText()).isEqualTo("Rutina Push E2E v2");
        assertThat(editado.body().path("exercises").get(0).path("plannedSets").asInt()).isEqualTo(5);

        ObjectNode invalidaPayload = JSON.createObjectNode().put("name", "Rutina Invalida");
        invalidaPayload.putArray("exercises");
        Res sinEjercicios = call("POST", "/api/v1/workouts/routines", token, invalidaPayload.toString());
        assertThat(sinEjercicios.status()).isEqualTo(400);

        Res ajena = call("GET", "/api/v1/workouts/routines/" + routineId,
                "token-basura-no-valido", null);
        assertThat(ajena.status()).isEqualTo(401);

        Res borrada = call("DELETE", "/api/v1/workouts/routines/" + routineId, token, null);
        assertThat(borrada.status()).isEqualTo(204);

        Res trasBorrar = call("GET", "/api/v1/workouts/routines/" + routineId, token, null);
        assertThat(trasBorrar.status()).isEqualTo(404);
    }

    @Test
    @Order(13)
    @DisplayName("sesion desde rutina: copia ejercicios y permite agregar/quitar con series")
    void sesionDesdeRutina() {
        // nueva rutina con 1 ejercicio planificado
        ObjectNode rutinaPayload = JSON.createObjectNode()
                .put("name", "Rutina Sesiones E2E")
                .put("sessionType", "HYPERTROPHY")
                .put("muscleLabel", "PUSH");
        rutinaPayload.putArray("exercises")
                .addObject()
                .put("exerciseId", exerciseId)
                .put("plannedSets", 3);
        Res rutinaCreada = call("POST", "/api/v1/workouts/routines", token, rutinaPayload.toString());
        assertThat(rutinaCreada.status()).isEqualTo(201);
        String rid = rutinaCreada.body().path("id").asText();

        // iniciar sesion desde la rutina: muscle/sessionType y ejercicios se heredan
        Res sesion = call("POST", "/api/v1/workouts/sessions", token, JSON.createObjectNode()
                .put("performedAt", java.time.LocalDate.now().toString())
                .put("routineId", rid)
                .toString());
        assertThat(sesion.status()).isEqualTo(201);
        sessionId = sesion.body().path("id").asText();
        assertThat(sesion.body().path("routineName").asText()).isEqualTo("Rutina Sesiones E2E");
        assertThat(sesion.body().path("muscleLabel").asText()).isEqualTo("PUSH");
        assertThat(sesion.body().path("sessionType").asText()).isEqualTo("HYPERTROPHY");
        assertThat(sesion.body().path("exercises").size()).isEqualTo(1);
        assertThat(sesion.body().path("exercises").get(0).path("exerciseName").asText()).isEqualTo(exerciseName);
        assertThat(sesion.body().path("exercises").get(0).path("plannedSets").asInt()).isEqualTo(3);

        // agregar un segundo ejercicio del catalogo
        Res otroEjercicio = call("POST", "/api/v1/exercises", token, JSON.createObjectNode()
                .put("name", "Sentadilla E2E " + UUID.randomUUID().toString().substring(0, 8))
                .put("type", "STRENGTH")
                .put("primaryMuscle", "QUADRICEPS")
                .put("equipment", "BARBELL")
                .toString());
        assertThat(otroEjercicio.status()).isBetween(200, 299);
        String otroId = otroEjercicio.body().path("id").asText();

        Res agregado = call("POST", "/api/v1/workouts/sessions/" + sessionId + "/exercises", token,
                JSON.createObjectNode()
                        .put("exerciseId", otroId)
                        .put("plannedSets", 5)
                        .toString());
        assertThat(agregado.status()).isEqualTo(201);
        assertThat(agregado.body().path("plannedSets").asInt()).isEqualTo(5);
        assertThat(agregado.body().path("exerciseName").asText()).contains("Sentadilla E2E");

        // agregar el mismo ejercicio no duplica
        Res duplicado = call("POST", "/api/v1/workouts/sessions/" + sessionId + "/exercises", token,
                JSON.createObjectNode()
                        .put("exerciseId", otroId)
                        .put("plannedSets", 5)
                        .toString());
        assertThat(duplicado.status()).isEqualTo(201);

        Res detalle = call("GET", "/api/v1/workouts/sessions/" + sessionId, token, null);
        assertThat(detalle.status()).isEqualTo(200);
        assertThat(detalle.body().path("exercises").size()).isEqualTo(2);

        // registrar una serie en el segundo ejercicio y luego quitarlo: borra sus series
        Res serie = call("POST", "/api/v1/workouts/sessions/" + sessionId + "/sets", token, JSON.createObjectNode()
                .put("exerciseId", otroId)
                .put("exerciseName", "Sentadilla E2E")
                .put("setNumber", 1)
                .put("weightKg", 100)
                .put("reps", 5)
                .put("rir", 1)
                .toString());
        assertThat(serie.status()).isBetween(200, 299);

        Res quitado = call("DELETE", "/api/v1/workouts/sessions/" + sessionId + "/exercises/" + otroId, token, null);
        assertThat(quitado.status()).isEqualTo(204);

        Res trasQuitar = call("GET", "/api/v1/workouts/sessions/" + sessionId, token, null);
        assertThat(trasQuitar.status()).isEqualTo(200);
        assertThat(trasQuitar.body().path("exercises").size()).isEqualTo(1);
        assertThat(trasQuitar.body().path("sets").size()).isEqualTo(0);

        // limpiar la rutina de prueba
        Res rutinaBorrada = call("DELETE", "/api/v1/workouts/routines/" + rid, token, null);
        assertThat(rutinaBorrada.status()).isEqualTo(204);
    }
}
