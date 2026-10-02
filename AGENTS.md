# AGENTS.md

FitTracker: microservicios Spring Cloud (Java 21, Spring Boot 3.5.14, Spring Cloud 2025.0.0, Lombok) + frontend Angular 17. Sin README; este archivo es la guia.

## Layout

Reactor raiz en `pom.xml` (16 modulos). El paquete base es `com.fittracker.*` (sin underscore). Arquitectura N-Tier estricta por servicio (`persistence`, `presentation`, `service`, `utils`, `configuration`) — NO Hexagonal/Clean.

- `infraestructura/`: `config-server` (:8888), `discovery-server` (Eureka :8761), `api-gateway` (:8222)
- `servicios-negocio/`: `auth-service` (:8080), `calculator-service` (:8081), `routine-service` (:8082), `user-service` (:8083), `workout-service` (:8084), `common-exceptions` (lib compartida)
- `messaging/rabbitmq` (`messaging-rabbitmq`): topology RabbitMQ compartida + `Jackson2JsonMessageConverter` — registrada como autoconfig via `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (si no, NO se carga: el paquete queda fuera del component-scan)
- `testing/e2e-tests`: suite E2E backend contra el stack Docker (gated)
- `frontend/`: Angular 17 standalone, SCSS, Karma/Jasmine + Playwright
- `docker/`: `docker-compose.yml`, `.env` (Postgres/JWT/puertos, gitignorado), `initdb/init.sql` (DBs `auth`, `user`, `routine`, `workout`)
- `config-server/` (raiz): espejos locales de yamls del repo externo — la config real vive en `https://github.com/kevinshz/config-fit-tracker.git`

**OJO**: `auth-service/pom.xml` tiene `parent = spring-boot-starter-parent:3.5.4` (NO hereda el pom raiz) — toda version gestionada (springdoc, messaging-rabbitmq, etc.) debe ir **explicita** ahi.

## Comandos

Backend (desde la raiz del repo):

- Build completo: `mvn clean package -DskipTests`
- Tests unitarios: `mvn test` (los integrados se auto-saltan por flags, ver abajo)
- Tests con Testcontainers: `mvn test "-Dtestcontainers.enabled=true"` (comillas en PowerShell: los `-D` sin comillas se pierden)
- E2E backend (stack Docker arriba): `mvn test -pl testing/e2e-tests "-De2e.enabled=true"`
- Un modulo: `mvn test -pl servicios-negocio/workout-service`
- JaCoCo: `mvn verify` (reporte en `target/site/jacoco/` de cada modulo)

Frontend (`frontend/`):

- Unit: `npm test -- --watch=false --browsers=ChromeHeadless` (47 tests)
- E2E: `npx playwright test` (12 tests: auth 4, workout 2, routines 6; requiere stack Docker arriba, backend en :8222 y frontend en :4200)
- Build: `npm run build` / contenedor via compose — **`tsconfig.app.json` debe excluir `src/**/*.spec.ts`** (si no, `ng build` compila los specs y falla con `Cannot find name 'describe'`)

Stack completo: `docker compose up -d --build` desde `docker/` (11 contenedores). Orden: discovery -> config -> servicios -> gateway (lo impone `service_healthy`).

## Runtime

- Puertos: gateway `:8222`, auth `:8080`, calculator `:8081`, routine `:8082`, user `:8083`, workout `:8084`, eureka `:8761`, config `:8888`, postgres `:5432`, frontend `:4200`
- Frontend (prod, nginx) proxya `/auth/` y `/api/` a `api-gateway:8222`; `environment.ts` usa `apiBaseUrl: 'http://localhost:8222'` (el browser llama al gateway directo)
- CORS: `spring.cloud.gateway.globalcors` en `infraestructura/api-gateway/src/main/resources/application.yaml` + `JwtValidationFilter` salta OPTIONS (preflight) — sin esto el frontend no puede llamar al API
- JWT: `sub` = UUID del usuario, claims `userId`, `email`, `roles`. Ambos lados derivan la clave = `jwtSecret.getBytes(UTF_8)`. El gateway valida y propaga `X-User-Id`/`X-User-Email`/`X-User-Roles`; los servicios confian en esos headers. Skip del filtro: `/auth/`, `/actuator/`, `/eureka/`, `/swagger-ui*`, `/v3/api-docs`, `/api-docs/`
- Rutas gateway (en el yaml local, NO en el repo externo): `/auth/**`, `/calc`es: `/api/v1/calculator/**`, `/api/v1/exercises/**` -> ROUTINE, `/api/v1/users/**` -> USER, `/api/v1/workouts/**` -> WORKOUT, mas 5 rutas `RewritePath` `/api-docs/{svc}/**` para Swagger agregado
- `spring.cloud.config.enabled: false` en el gateway a proposito: el repo externo `config-fit-tracker` solo tiene `api-gateway.yaml` obsoleto (solo ruta `/auth`) y `security-service.yaml`; si se actualiza el repo, reactivar. Los demas servicios tienen config local y config import `optional:configserver` (los 404 del config server son esperados)
- RabbitMQ: auth publica `user.created` en `fittracker.exchange`; user-service consume `user.created.queue` y crea el perfil (asincrono: `/users/me` puede dar 404/400 los primeros segundos tras registrarse). Workout publica `workout.session.completed`
- Frontend maneja el evento del perfil con reintentos; el backend E2E reintenta `/users/me` hasta 20s

## Features (estado)

- **Catalogo en espanol**: `routine-service/src/main/resources/data/exercise-names-es.json` (1318 nombres ES, clave = externalId). `DataInitializer.run` (@Transactional) siembra y luego aplica `translateCatalog()` idempotente: `name`=ES, ingles preservado en `aliases` (unicos), colisiones se saltan con log. Verificado: reinicio dice "Catalogo ya traducido; sin cambios"; búsquedas EN (`q=bicep`) siguen funcionando via alias. Para regenerar: `mvn test -pl servicios-negocio/routine-service` (DataInitializerTest valida cobertura exacta)
- **Mis Rutinas** (planner de rutinas): backend workout-service `RoutineController` (`/api/v1/workouts/routines` CRUD, ejercicios planificados con `plannedSets`) + `POST /sessions` acepta `routineId` (copia ejercicios de la rutina, hereda muscleLabel/sessionType, guarda `routineName`) + `POST|DELETE /sessions/{id}/exercises[/{exerciseId}]` (201/204, sin duplicar). Frontend: `/routines` (list/form), navbar tab "Rutinas" (reemplazo el enlace roto `/workout/history`), selector de rutina en `/workout/start` (`?routine=` preselecciona y auto-rellena tipo/muscle), sesión muestra `routineName` como título, modal "Agregar ejercicio" y confirmación al quitar ejercicio con series registradas ("¿Seguro que deseas eliminar este ejercicio?..."), columna REFERENCIA ANTERIOR con el rendimiento de la última sesión
- Etiquetas ES de enums en `frontend/src/app/core/models/enums.ts` (`MUSCLE_GROUP_LABELS`, `EQUIPMENT_LABELS`, `EXERCISE_TYPE_LABELS` + helpers `muscleGroupLabel`/`equipmentLabel`/`exerciseTypeLabel`); los templates usan campos del componente (`readonly labelMuscle = muscleGroupLabel`) porque Angular no permite llamar funciones importadas directamente en el template

## Testing (convenciones)

- Gating por propiedad del sistema, activo en la **clase concreta** (ponerlo en una clase abstracta NO funciona: JUnit lo ignora):
  - Testcontainers: `@EnabledIfSystemProperty(named="testcontainers.enabled", matches="true")` en las clases de integracion (auth/user/routine/workout)
  - E2E: `@EnabledIfSystemProperty(named="e2e.enabled", matches="true")`
- Base Testcontainers por servicio: `AbstractPostgresIntegrationTest` con `@ActiveProfiles("test")`, `postgres:16-alpine` static `@Container`, `@DynamicPropertySource` (url/user/pass/**driver-class-name=org.postgresql.Driver**/flyway off/ddl-auto create-drop). `application-test.yaml` desactiva config/eureka
- `src/test/resources/docker-java.properties` (`api.version=1.44`) en auth/user/routine/workout + `surefire systemPropertyVariables` en el pom raiz (Docker 29 exige API >= 1.44)
- H2 solo para tests `@DataJpaTest`/flow sin Docker; `@DataJpaTest` necesita `@AutoConfigureTestDatabase(replace = Replace.NONE)`
- Tests puros (sin Docker, `mvn test` reactor): calculator 35, auth 19, gateway 10, workout 59, routine 14, user 8, common-exceptions 5, messaging 3 (+ config 1, discovery 1) -> **155 unit**. Integracion Testcontainers gated: auth 16, user 13, routine 15, workout 18 = **62** (bajo flag: auth 35, user 21, calculator 35, routine 29, workout 77). E2E backend **13**. Frontend Karma **47**. Playwright **12**
- Swagger/Springdoc **2.8.14** (obligatorio para Boot 3.5/Spring 6.2 — 2.6.0 revienta con `NoSuchMethodError: ControllerAdviceBean.<init>(Object)`). Docs: cada servicio en `:puerto/v3/api-docs` y agregado en `:8222/swagger-ui.html` (5 urls via `/api-docs/{svc}/**`)
- `SchemaDumpTest` (auth/user/routine/workout): exporta DDL real a `target/db-migration/V1__init.sql` (borrar el archivo previo antes de regenerar); las migraciones viven en `src/main/resources/db/migration/V1__init.sql`
- DTOs: calculator usa registros con `getXxx()`; el resto de servicios registros con `xxx()`. Enums clave: `ExerciseType` = STRENGTH/CARDIO/MOBILITY; `Equipment` usa BARBELL (no "barra"); `MuscleLabel` = CHEST_BACK/LEGS/PUSH/PULL/SHOULDERS_ARMS/FULL_BODY; `SessionType` = STRENGTH/HYPERTROPHY. Codigo duplicado en ejercicios -> 400 "ya existe un ejercicio llamado..."
- Contratos HTTP: registro **201**, login 200, duplicado 409, credenciales malas 401, validacion 400, gateway sin/bad token **401**
- PowerShell 5.1: `Set-Content -Encoding UTF8` escribe BOM (javac falla) -> usar `New-Object System.Text.UTF8Encoding($false)`; comillas dobles dentro de comillas simples en `-Pattern`

## Convenciones

- Comentarios en espanol, estilos de negocio en espanol
- Lombok `provided` + `annotationProcessorPaths` en `maven-compiler-plugin` — mantener ambos
- Frontend: standalone components, lazy routes con `loadComponent`, `authGuard` en la raiz de rutas protegidas, Reactive Forms, interceptor functional `authInterceptor` (no adjunta token en `/auth/`), localStorage key `fittracker_token`
- Frontend modales: sin `.modal-content` — **`.modal-dialog` global de Bootstrap trae `pointer-events: none`** (espera que `.modal-content` restaure `auto`); todo modal propio necesita `pointer-events: auto` en su `.modal-dialog` (lo tienen workout-session y routine-list; si se anade otro modal, repetirlo)

## Bugs corregidos (historial, no reintroducir)

1. JWT: clave derivada distinta auth (Base64) vs gateway (UTF-8) y `sub`=email vs UUID — unificado a UTF-8 + `sub`=userId
2. Repo config externo con rutas obsoletas pisando el gateway -> `spring.cloud.config.enabled: false`
3. `exercises.json` con 6 nombres duplicados -> `DataInitializer` dedupe (siembra 1318)
4. calculator-service sin `application.yaml` (arranque caia) y sin 400 para JSON invalido
5. RabbitMQ: topology declarada pero jamas cargada -> autoconfig imports + `messaging-rabbitmq` como dependencia de auth y workout
6. Dockerfiles copiando `infraestructura/src` inexistente y pom.xml parciales -> `COPY . .` + cache de maven + `.dockerignore`
7. user/routine sin dependencias Flyway pese a config Flyway; sin migraciones -> `V1__init.sql` generadas
8. Gateway sin CORS/globalcors y filtro JWT rechazando preflight OPTIONS -> frontend imposible
9. springdoc 2.6.0 incompatible con Boot 3.5 -> 2.8.14 (y version explicita en auth pom)
10. jwt secret/expiration + healthcheck de gateway ausentes en compose (frontend dependia de `service_healthy`)
11. Modales propios in clicables en Playwright: Bootstrap `.modal-dialog` tiene `pointer-events: none` y nosotros no usamos `.modal-content` -> restaurar `pointer-events: auto` (workout-session, routine-list)
12. `tsconfig.app.json` sin `exclude` de specs -> `ng build` compilaba los `.spec.ts` y reventaba con `Cannot find name 'describe'` (los tests viven solo en Karma)

## Pendientes conocidos

- Repo externo `kevinshz/config-fit-tracker` desactualizado (empujar `api-gateway.yaml` actual y reactivar config import del gateway)
- `GlobalExceptionHandler` de `common-exceptions` nunca se registra como `@RestControllerAdvice`
- Padre de `auth-service` = spring-boot-starter-parent **3.5.4** (resto 3.5.14) — alinear cuando se toque
- Sin repositorio git todavia
