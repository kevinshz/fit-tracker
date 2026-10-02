package com.fittracker.workout;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Utilidad: exporta el esquema JPA (Hibernate) a un script SQL.
 * Genera target/db-migration/V1__init.sql para usarlo como migracion Flyway.
 * Requiere -Dtestcontainers.enabled=true; borrar el archivo previo antes de regenerar
 * (Hibernate anexa si el archivo ya existe).
 */
@Testcontainers
@EnabledIfSystemProperty(named = "testcontainers.enabled", matches = "true",
        disabledReason = "Requiere Docker (usar -Dtestcontainers.enabled=true)")
@SpringBootTest
@DisplayName("SchemaDump - Exporta DDL a target/db-migration/V1__init.sql")
class SchemaDumpTest extends AbstractPostgresIntegrationTest {

    @DynamicPropertySource
    static void scriptGeneration(DynamicPropertyRegistry registry) {
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.properties.jakarta.persistence.schema-generation.database.action",
                () -> "create");
        registry.add("spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action",
                () -> "create");
        registry.add("spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target",
                () -> "target/db-migration/V1__init.sql");
    }

    @org.junit.jupiter.api.BeforeAll
    static void limpiarScriptAnterior() throws Exception {
        java.nio.file.Files.deleteIfExists(java.nio.file.Path.of("target/db-migration/V1__init.sql"));
    }

    @Test
    @DisplayName("Genera el script SQL del esquema")
    void dumpSchema() throws Exception {
        var file = new java.io.File("target/db-migration/V1__init.sql");
        assertThat(file).exists();
        assertThat(file.length()).isGreaterThan(0);
    }
}

