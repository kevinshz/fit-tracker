package com.fittracker.auth.configuration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public String generateToken(String userId, UserDetails userDetails, Map<String, Object> extraClaims) {
        Map<String, Object> claims = new HashMap<>(extraClaims);
        // Contrato con el api-gateway: sub = userId, email como claim
        claims.put("userId", userId);
        claims.put("email", userDetails.getUsername());
        return buildToken(claims, userId, userDetails, jwtExpiration);
    }

    public String extractUserId(String token) {
        return extractClaim(token, claims -> {
            String userId = claims.get("userId", String.class);
            return userId != null ? userId : claims.getSubject();
        });
    }

    public String extractEmail(String token) {
        return extractClaim(token, claims -> {
            String email = claims.get("email", String.class);
            return email != null ? email : claims.getSubject();
        });
    }

    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List<?>) {
            return ((List<?>) rolesObj).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token JWT inválido: {}", e.getMessage());
            return false;
        }
    }

    private String buildToken(Map<String, Object> claims, String userId, UserDetails userDetails, long expiration) {
        Date now = new Date();
        return Jwts.builder()
                .claims(claims)
                .subject(userId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(getSignInKey())
                .compact();
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private SecretKey getSignInKey() {
        // Misma derivacion que el api-gateway: clave HMAC = bytes UTF-8 del secreto
        byte[] keyBytes = jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
