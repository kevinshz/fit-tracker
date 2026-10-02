package com.fittracker.auth.service.implementation;

import com.fittracker.auth.configuration.JwtService;
import com.fittracker.auth.persistence.entity.Role;
import com.fittracker.auth.persistence.entity.User;
import com.fittracker.auth.persistence.repository.RoleRepository;
import com.fittracker.auth.persistence.repository.UserRepository;
import com.fittracker.auth.presentation.dto.*;
import com.fittracker.auth.service.exception.EmailAlreadyExistsException;
import com.fittracker.auth.service.messaging.UserEventProducer;
import com.fittracker.auth.utils.mapper.AuthMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl - Tests Unitarios con Mockito")
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserEventProducer userEventProducer;
    @Mock private AuthMapper authMapper;
    @Mock private UserDetailsServiceImpl userDetailsService;

    @InjectMocks private AuthServiceImpl authService;

    private Role buildRole() {
        Role role = new Role();
        role.setName("ROLE_USER");
        return role;
    }

    private User buildUser(String email) {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash("encodedPassword")
                .name("Test User")
                .enabled(true)
                .roles(Set.of(buildRole()))
                .build();
        return user;
    }

    @Test
    @DisplayName("register: happy path - crea usuario, envia evento, genera token")
    void register_success() {
        RegisterRequest request = new RegisterRequest("test@example.com", "pass123", "Test");
        User savedUser = buildUser("test@example.com");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(buildRole()));
        when(passwordEncoder.encode("pass123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(anyString(), any(UserDetails.class), anyMap())).thenReturn("jwt-token");
        when(authMapper.toAuthResponse(any(User.class), anyString(), anyList()))
                .thenReturn(new AuthResponse("jwt-token", savedUser.getId(), "test@example.com", List.of("ROLE_USER")));

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.email()).isEqualTo("test@example.com");
        verify(userEventProducer).sendUserCreatedEvent(anyMap());
    }

    @Test
    @DisplayName("register: email duplicado lanza EmailAlreadyExistsException")
    void register_duplicateEmail_throws() {
        RegisterRequest request = new RegisterRequest("test@example.com", "pass123", "Test");
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("test@example.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: ROLE_USER no encontrado lanza IllegalStateException")
    void register_missingRole_throws() {
        RegisterRequest request = new RegisterRequest("test@example.com", "pass123", "Test");
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalStateException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login: happy path - autentica y genera token")
    void login_success() {
        LoginRequest request = new LoginRequest("test@example.com", "pass123");
        User user = buildUser("test@example.com");
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("test@example.com")
                .password("encodedPassword")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(anyString(), any(UserDetails.class), anyMap())).thenReturn("jwt-token");
        when(authMapper.toAuthResponse(any(User.class), anyString(), anyList()))
                .thenReturn(new AuthResponse("jwt-token", user.getId(), "test@example.com", List.of("ROLE_USER")));

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("validateToken: token valido retorna valid=true")
    void validateToken_valid() {
        String token = "valid-token";
        User user = buildUser("test@example.com");
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("test@example.com")
                .password("encodedPassword")
                .authorities(List.of())
                .build();

        when(jwtService.extractEmail(token)).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid(anyString(), any(UserDetails.class))).thenReturn(true);

        TokenValidationResponse response = authService.validateToken(token);

        assertThat(response.valid()).isTrue();
        assertThat(response.email()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("validateToken: usuario no encontrado retorna valid=false")
    void validateToken_userNotFound() {
        String token = "some-token";
        when(jwtService.extractEmail(token)).thenReturn("unknown@example.com");
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        TokenValidationResponse response = authService.validateToken(token);

        assertThat(response.valid()).isFalse();
        assertThat(response.userId()).isNull();
    }

    @Test
    @DisplayName("validateToken: excepcion retorna valid=false")
    void validateToken_exception() {
        String token = "corrupt-token";
        when(jwtService.extractEmail(token)).thenThrow(new RuntimeException("parse error"));

        TokenValidationResponse response = authService.validateToken(token);

        assertThat(response.valid()).isFalse();
    }

    @Test
    @DisplayName("refreshToken: token valido genera nuevo token")
    void refreshToken_valid() {
        String oldToken = "old-token";
        User user = buildUser("test@example.com");
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("test@example.com")
                .password("encodedPassword")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        when(jwtService.extractEmail(oldToken)).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid(anyString(), any(UserDetails.class))).thenReturn(true);
        when(jwtService.generateToken(anyString(), any(UserDetails.class), anyMap())).thenReturn("new-token");
        when(authMapper.toAuthResponse(any(User.class), anyString(), anyList()))
                .thenReturn(new AuthResponse("new-token", user.getId(), "test@example.com", List.of("ROLE_USER")));

        AuthResponse response = authService.refreshToken(oldToken);

        assertThat(response.token()).isEqualTo("new-token");
    }

    @Test
    @DisplayName("refreshToken: token invalido lanza IllegalArgumentException")
    void refreshToken_invalid() {
        String oldToken = "expired-token";
        User user = buildUser("test@example.com");
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("test@example.com")
                .password("encodedPassword")
                .authorities(List.of())
                .build();

        when(jwtService.extractEmail(oldToken)).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid(anyString(), any(UserDetails.class))).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken(oldToken))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
