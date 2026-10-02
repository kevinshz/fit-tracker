package com.fittracker.workout;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

/**
 * Base para tests de integracion con PostgreSQL real via Testcontainers.
 * Solo se ejecuta con -Dtestcontainers.enabled=true (requiere Docker).
 */
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "testcontainers.enabled", matches = "true",
        disabledReason = "Requiere Docker en ejecucion (usar -Dtestcontainers.enabled=true)")
public abstract class AbstractPostgresIntegrationTest {

    @Container
    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("workout_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }
}
