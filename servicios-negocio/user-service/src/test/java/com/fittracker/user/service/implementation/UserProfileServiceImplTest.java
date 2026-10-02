package com.fittracker.user.service.implementation;

import com.fittracker.common.exceptions.ResourceNotFoundException;
import com.fittracker.user.persistence.entity.UserProfile;
import com.fittracker.user.persistence.repository.UserProfileRepository;
import com.fittracker.user.presentation.dto.CreateProfileRequest;
import com.fittracker.user.presentation.dto.UpdateProfileRequest;
import com.fittracker.user.service.exception.ProfileAlreadyExistsException;
import com.fittracker.user.utils.mapper.UserProfileMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileServiceImpl - Tests Unitarios")
class UserProfileServiceImplTest {

    @Mock private UserProfileRepository repository;
    @Spy private UserProfileMapper mapper = new UserProfileMapper();

    @InjectMocks private UserProfileServiceImpl service;

    private final UUID userId = UUID.randomUUID();

    private UserProfile buildProfile() {
        return UserProfile.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .email("test@example.com")
                .name("Test")
                .build();
    }

    @Nested
    @DisplayName("createProfile")
    class CreateProfile {

        @Test
        @DisplayName("Happy path crea el perfil")
        void success() {
            var request = new CreateProfileRequest(userId, "test@example.com", "Test",
                    70.0, 175.0, null, null, null, null);
            when(repository.existsByUserId(userId)).thenReturn(false);
            when(repository.save(any(UserProfile.class))).thenAnswer(inv -> {
                UserProfile p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });

            var response = service.createProfile(request);

            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.email()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Perfil duplicado lanza ProfileAlreadyExistsException")
        void duplicate_throws() {
            var request = new CreateProfileRequest(userId, "test@example.com", "Test",
                    null, null, null, null, null, null);
            when(repository.existsByUserId(userId)).thenReturn(true);

            assertThatThrownBy(() -> service.createProfile(request))
                    .isInstanceOf(ProfileAlreadyExistsException.class);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getProfileByUserId")
    class GetProfile {

        @Test
        @DisplayName("Retorna el perfil encontrado")
        void found() {
            when(repository.findByUserId(userId)).thenReturn(Optional.of(buildProfile()));

            var response = service.getProfileByUserId(userId);

            assertThat(response.userId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("No encontrado lanza ResourceNotFoundException")
        void notFound_throws() {
            when(repository.findByUserId(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getProfileByUserId(userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfile {

        @Test
        @DisplayName("Actualiza solo los campos no nulos")
        void partialUpdate() {
            UserProfile existing = buildProfile();
            existing.setBodyWeight(80.0);
            when(repository.findByUserId(userId)).thenReturn(Optional.of(existing));
            when(repository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

            var request = new UpdateProfileRequest("Nuevo nombre", 75.5, null, null, null, null, null);
            var response = service.updateProfile(userId, request);

            assertThat(response.name()).isEqualTo("Nuevo nombre");
            assertThat(response.bodyWeight()).isEqualTo(75.5);
            assertThat(response.height()).isNull();
        }

        @Test
        @DisplayName("Perfil no encontrado lanza ResourceNotFoundException")
        void notFound_throws() {
            when(repository.findByUserId(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateProfile(userId, new UpdateProfileRequest(
                    "x", null, null, null, null, null, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteProfile")
    class DeleteProfile {

        @Test
        @DisplayName("Elimina el perfil existente")
        void success() {
            UserProfile existing = buildProfile();
            when(repository.findByUserId(userId)).thenReturn(Optional.of(existing));

            service.deleteProfile(userId);

            verify(repository).delete(existing);
        }

        @Test
        @DisplayName("Perfil no encontrado lanza ResourceNotFoundException")
        void notFound_throws() {
            when(repository.findByUserId(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteProfile(userId))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(repository, never()).delete(any());
        }
    }
}
