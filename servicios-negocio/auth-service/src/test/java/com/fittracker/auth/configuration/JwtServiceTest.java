package com.fittracker.auth.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService - Tests Unitarios")
class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET = "dGVzdHNlY3JldGtleWZvcmp3dGd2ZXJpZnlzaWduYXR1cmVrZXk=";
    private static final long EXPIRATION = 3600000L;
    private static final String USER_ID = "123e4567-e89b-12d3-a456-426614174000";

    private final UserDetails userDetails = User.builder()
            .username("test@example.com")
            .password("hashedPassword")
            .authorities(List.of())
            .build();

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION);
    }

    @Test
    @DisplayName("generateToken crea token no vacio")
    void generateToken_producesToken() {
        String token = jwtService.generateToken(USER_ID, userDetails, Map.of("roles", List.of("ROLE_USER")));
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("extractEmail retorna el claim email")
    void extractEmail_returnsEmailClaim() {
        String token = jwtService.generateToken(USER_ID, userDetails, Map.of("roles", List.of("ROLE_USER")));
        assertThat(jwtService.extractEmail(token)).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("extractRoles retorna los roles del claim")
    void extractRoles_returnsRoles() {
        String token = jwtService.generateToken(USER_ID, userDetails, Map.of("roles", List.of("ROLE_USER", "ROLE_ADMIN")));
        List<String> roles = jwtService.extractRoles(token);
        assertThat(roles).containsExactly("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("isTokenValid retorna true para token valido con usuario correcto")
    void isTokenValid_trueForValidToken() {
        String token = jwtService.generateToken(USER_ID, userDetails, Map.of("roles", List.of("ROLE_USER")));
        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid retorna false si el usuario no coincide")
    void isTokenValid_falseForWrongUser() {
        String token = jwtService.generateToken(USER_ID, userDetails, Map.of("roles", List.of("ROLE_USER")));
        UserDetails otherUser = User.builder()
                .username("other@example.com")
                .password("pass")
                .authorities(List.of())
                .build();
        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    @DisplayName("validateToken retorna true para token valido")
    void validateToken_trueForValid() {
        String token = jwtService.generateToken(USER_ID, userDetails, Map.of("roles", List.of("ROLE_USER")));
        assertThat(jwtService.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken retorna false para token corrupto")
    void validateToken_falseForCorrupted() {
        assertThat(jwtService.validateToken("not-a-jwt-token")).isFalse();
    }

    @Test
    @DisplayName("validateToken retorna false para token vacio")
    void validateToken_falseForEmpty() {
        assertThat(jwtService.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("validateToken retorna false para token firmado con otra clave")
    void validateToken_falseForWrongSignature() {
        JwtService otherService = new JwtService();
        ReflectionTestUtils.setField(otherService, "jwtSecret",
                "b3RoZXJzZWNyZXRrZXl0aGF0aXNsb25nZW5vdWdoZm9yaG1hY3NoYTI1NmtleQ==");
        ReflectionTestUtils.setField(otherService, "jwtExpiration", EXPIRATION);
        String token = otherService.generateToken(USER_ID, userDetails, Map.of());
        assertThat(jwtService.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("extractUserId retorna valor del claim userId si existe")
    void extractUserId_returnsClaim() {
        String token = jwtService.generateToken(USER_ID, userDetails, Map.of(
                "roles", List.of("ROLE_USER"),
                "userId", "123e4567-e89b-12d3-a456-426614174000"
        ));
        assertThat(jwtService.extractUserId(token)).isEqualTo("123e4567-e89b-12d3-a456-426614174000");
    }
}
