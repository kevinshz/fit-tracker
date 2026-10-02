package com.fittracker.auth.persistence.repository;

import com.fittracker.auth.AbstractPostgresIntegrationTest;
import com.fittracker.auth.persistence.entity.Role;
import com.fittracker.auth.persistence.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@EnabledIfSystemProperty(named = "testcontainers.enabled", matches = "true",
        disabledReason = "Requiere Docker (usar -Dtestcontainers.enabled=true)")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("UserRepository - Integracion con PostgreSQL (Testcontainers)")
class UserRepositoryTest extends AbstractPostgresIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;

    private Role buildRole(String name) {
        return roleRepository.save(Role.builder().name(name).build());
    }

    private User buildUser(String email) {
        return User.builder()
                .email(email)
                .passwordHash("$2a$10$hashedpassword")
                .name("Test")
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("Guardar y recuperar usuario por email")
    void saveAndFindByEmail() {
        buildRole("ROLE_USER");
        Role role = roleRepository.findByName("ROLE_USER").orElseThrow();
        User user = buildUser("find@example.com");
        user.setRoles(Set.of(role));
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("find@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test");
        assertThat(found.get().getRoles()).hasSize(1);
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("existsByEmail retorna true/false correctamente")
    void existsByEmail() {
        buildUser("exists@example.com");
        userRepository.save(buildUser("exists@example.com"));

        assertThat(userRepository.existsByEmail("exists@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("absent@example.com")).isFalse();
    }

    @Test
    @DisplayName("Email duplicado viola restriccion unica")
    void duplicateEmail_violatesUniqueConstraint() {
        userRepository.save(buildUser("dup@example.com"));

        assertThatThrownBy(() -> {
            userRepository.save(buildUser("dup@example.com"));
            userRepository.flush();
        }).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Email nulo viola not-null")
    void nullEmail_violatesNotNull() {
        User user = buildUser("temp@example.com");
        user.setEmail(null);

        assertThatThrownBy(() -> {
            userRepository.save(user);
            userRepository.flush();
        }).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("findByEmail en inexistente retorna empty")
    void findByEmail_absent_returnsEmpty() {
        assertThat(userRepository.findByEmail("nobody@example.com")).isEmpty();
    }

    @Test
    @DisplayName("Roles persisten y se asocian via user_roles")
    void rolesPersisted() {
        buildRole("ROLE_USER");
        buildRole("ROLE_ADMIN");
        Role user = roleRepository.findByName("ROLE_USER").orElseThrow();
        Role admin = roleRepository.findByName("ROLE_ADMIN").orElseThrow();

        User u = buildUser("multi@example.com");
        u.setRoles(Set.of(user, admin));
        userRepository.save(u);

        var found = userRepository.findByEmail("multi@example.com").orElseThrow();
        assertThat(found.getRoles())
                .extracting(Role::getName)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("Id autogenerado como UUID")
    void generatesUuid() {
        User saved = userRepository.save(buildUser("uuid@example.com"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).isInstanceOf(UUID.class);
    }
}
