package com.fittracker.api_gateway.filters;

import com.fittracker.api_gateway.configuration.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("JwtValidationFilter - Tests Unitarios")
class JwtValidationFilterTest {

    private static final String SECRET = "c2VjdXJpdHlzZWNyZXRrZXl0aGF0aXNsb25nZW5vdWdoZm9yaG1hY3NoYTI1Ng==";

    private JwtValidationFilter filter;
    private JwtProperties jwtProperties;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret(SECRET);
        jwtProperties.setExpiration(3600000L);
        filter = new JwtValidationFilter(jwtProperties);
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    private String buildToken(String subject, String email, List<String> roles, Date expiration) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(subject)
                .claims(Map.of("email", email, "roles", roles))
                .issuedAt(new Date())
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    @Test
    @DisplayName("getOrder retorna -1 (ejecuta primero)")
    void getOrder_returnsMinusOne() {
        assertThat(filter.getOrder()).isEqualTo(-1);
    }

    @Test
    @DisplayName("Ruta /auth/** se omite sin validar token")
    void shouldSkip_authPath() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/auth/login").build());
        filter.filter(exchange, chain).block();
        verify(chain).filter(exchange);
    }

    @Test
    @DisplayName("Ruta /actuator/** se omite")
    void shouldSkip_actuatorPath() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/actuator/health").build());
        filter.filter(exchange, chain).block();
        verify(chain).filter(exchange);
    }

    @Test
    @DisplayName("Sin header Authorization retorna 401")
    void missingAuthHeader_returns401() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/routines").build());
        filter.filter(exchange, chain).block();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("Header sin Bearer retorna 401")
    void nonBearerHeader_returns401() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/routines")
                        .header(HttpHeaders.AUTHORIZATION, "Basic abc123")
                        .build());
        filter.filter(exchange, chain).block();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("Token valido: inyecta X-User-Id, X-User-Email, X-User-Roles y continua")
    void validToken_injectsHeaders() {
        String token = buildToken("user-uuid-123", "test@example.com",
                List.of("ROLE_USER", "ROLE_ADMIN"),
                new Date(System.currentTimeMillis() + 60000));

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/routines")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build());

        AtomicReference<String> userId = new AtomicReference<>();
        AtomicReference<String> email = new AtomicReference<>();
        AtomicReference<String> roles = new AtomicReference<>();

        when(chain.filter(any())).thenAnswer(invocation -> {
            var mutated = invocation.getArgument(0, org.springframework.web.server.ServerWebExchange.class);
            userId.set(mutated.getRequest().getHeaders().getFirst("X-User-Id"));
            email.set(mutated.getRequest().getHeaders().getFirst("X-User-Email"));
            roles.set(mutated.getRequest().getHeaders().getFirst("X-User-Roles"));
            return Mono.empty();
        });

        filter.filter(exchange, chain).block();

        assertThat(userId.get()).isEqualTo("user-uuid-123");
        assertThat(email.get()).isEqualTo("test@example.com");
        assertThat(roles.get()).isEqualTo("ROLE_USER,ROLE_ADMIN");
    }

    @Test
    @DisplayName("Token expirado retorna 401 'Token expirado'")
    void expiredToken_returns401() {
        String token = buildToken("user-123", "test@example.com",
                List.of("ROLE_USER"),
                new Date(System.currentTimeMillis() - 1000));

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/routines")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build());
        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("Token con firma incorrecta retorna 401")
    void badSignature_returns401() {
        String otherSecret = "b3RoZXJzZWNyZXRrZXl0aGF0aXNsb25nZW5vdWdoZm9yaG1hY3NoYTI1Nng=";
        SecretKey otherKey = Keys.hmacShaKeyFor(otherSecret.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("user-123")
                .claims(Map.of("email", "test@example.com", "roles", List.of("ROLE_USER")))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(otherKey)
                .compact();

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/routines")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build());
        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("Token basura retorna 401")
    void garbageToken_returns401() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/routines")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not.a.token")
                        .build());
        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("Token sin roles: X-User-Roles vacio")
    void tokenWithoutRoles_emptyRolesHeader() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("user-123")
                .claims(Map.of("email", "test@example.com"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(key)
                .compact();

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/routines")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build());

        AtomicReference<String> roles = new AtomicReference<>();
        when(chain.filter(any())).thenAnswer(invocation -> {
            var mutated = invocation.getArgument(0, org.springframework.web.server.ServerWebExchange.class);
            roles.set(mutated.getRequest().getHeaders().getFirst("X-User-Roles"));
            return Mono.empty();
        });

        filter.filter(exchange, chain).block();

        assertThat(roles.get()).isEmpty();
    }
}
