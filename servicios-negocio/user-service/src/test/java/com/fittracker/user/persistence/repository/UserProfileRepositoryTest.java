package com.fittracker.user.persistence.repository;

import com.fittracker.user.AbstractPostgresIntegrationTest;
import com.fittracker.user.persistence.entity.UserProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@EnabledIfSystemProperty(named = "testcontainers.enabled", matches = "true",
        disabledReason = "Requiere Docker (usar -Dtestcontainers.enabled=true)")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("UserProfileRepository - Integracion con PostgreSQL (Testcontainers)")
class UserProfileRepositoryTest extends AbstractPostgresIntegrationTest {

    @Autowired private UserProfileRepository repository;

    private UserProfile buildProfile(UUID userId, String email) {
        return UserProfile.builder()
                .userId(userId)
                .email(email)
                .name("Test")
                .bodyWeight(75.0)
                .height(178.0)
                .build();
    }

    @Test
    @DisplayName("Guardar y buscar perfil por userId")
    void saveAndFindByUserId() {
        UUID userId = UUID.randomUUID();
        repository.save(buildProfile(userId, "find@example.com"));

        var found = repository.findByUserId(userId);

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test");
        assertThat(found.get().getBodyWeight()).isEqualTo(75.0);
    }

    @Test
    @DisplayName("existsByUserId detecta duplicados")
    void existsByUserId() {
        UUID userId = UUID.randomUUID();
        repository.save(buildProfile(userId, "exists@example.com"));

        assertThat(repository.existsByUserId(userId)).isTrue();
        assertThat(repository.existsByUserId(UUID.randomUUID())).isFalse();
    }

    @Test
    @DisplayName("userId no unico puede dar problemas solo si se inserta dos veces")
    void saveTwoProfiles() {
        repository.save(buildProfile(UUID.randomUUID(), "a@example.com"));
        repository.save(buildProfile(UUID.randomUUID(), "b@example.com"));

        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("userId nulo viola not-null")
    void nullUserId_violatesNotNull() {
        UserProfile profile = buildProfile(UUID.randomUUID(), "x@example.com");
        profile.setUserId(null);

        assertThatThrownBy(() -> {
            repository.save(profile);
            repository.flush();
        }).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Buscar perfil inexistente retorna empty")
    void absent_returnsEmpty() {
        assertThat(repository.findByUserId(UUID.randomUUID())).isEmpty();
    }
}
