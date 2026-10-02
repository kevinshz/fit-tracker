package com.fittracker.auth.service.implementation;

import com.fittracker.auth.configuration.JwtService;
import com.fittracker.auth.persistence.entity.Role;
import com.fittracker.auth.persistence.entity.User;
import com.fittracker.auth.persistence.repository.RoleRepository;
import com.fittracker.auth.persistence.repository.UserRepository;
import com.fittracker.auth.presentation.dto.*;
import com.fittracker.auth.service.exception.EmailAlreadyExistsException;
import com.fittracker.auth.service.interfaces.AuthService;
import com.fittracker.auth.service.messaging.UserEventProducer;
import com.fittracker.auth.utils.mapper.AuthMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserEventProducer userEventProducer;
    private final AuthMapper authMapper;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    public AuthResponse register(RegisterRequest request) {
        log.info("Registrando nuevo usuario: {}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(
                    "El email ya está registrado: " + request.email());
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("ROLE_USER no encontrado"));

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .name(request.name())
                .enabled(true)
                .roles(Set.of(userRole))
                .build();

        User savedUser = userRepository.save(user);
        log.info("Usuario registrado exitosamente: {}", savedUser.getId());

        Map<String, Object> event = new HashMap<>();
        event.put("userId", savedUser.getId().toString());
        event.put("email", savedUser.getEmail());
        event.put("name", savedUser.getName());
        event.put("roles", List.of("ROLE_USER"));
        userEventProducer.sendUserCreatedEvent(event);

        String token = jwtService.generateToken(
                savedUser.getId().toString(),
                mapToUserDetails(savedUser),
                Map.of("roles", List.of("ROLE_USER"))
        );

        List<String> roles = savedUser.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        return authMapper.toAuthResponse(savedUser, token, roles);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Intento de login para: {}", request.email());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        String token = jwtService.generateToken(user.getId().toString(), userDetails, Map.of("roles", roles));

        log.info("Login exitoso para: {}", request.email());
        return authMapper.toAuthResponse(user, token, roles);
    }

    @Override
    public TokenValidationResponse validateToken(String token) {
        try {
            String email = jwtService.extractEmail(token);
            User user = userRepository.findByEmail(email)
                    .orElse(null);

            if (user == null) {
                return new TokenValidationResponse(false, null, null, List.of());
            }

            UserDetails userDetails = mapToUserDetails(user);
            boolean valid = jwtService.isTokenValid(token, userDetails);

            List<String> roles = user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toList());

            return new TokenValidationResponse(valid, user.getId(), user.getEmail(), roles);
        } catch (Exception e) {
            log.warn("Token inválido: {}", e.getMessage());
            return new TokenValidationResponse(false, null, null, List.of());
        }
    }

    @Override
    public AuthResponse refreshToken(String token) {
        String email = jwtService.extractEmail(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));

        UserDetails userDetails = mapToUserDetails(user);

        if (!jwtService.isTokenValid(token, userDetails)) {
            throw new IllegalArgumentException("Token inválido o expirado");
        }

        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        String newToken = jwtService.generateToken(user.getId().toString(), userDetails, Map.of("roles", roles));

        log.info("Token refrescado para: {}", email);
        return authMapper.toAuthResponse(user, newToken, roles);
    }

    private UserDetails mapToUserDetails(User user) {
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                user.isEnabled(),
                true,
                true,
                true,
                user.getRoles().stream()
                        .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                role.getName()))
                        .collect(Collectors.toSet())
        );
    }
}
