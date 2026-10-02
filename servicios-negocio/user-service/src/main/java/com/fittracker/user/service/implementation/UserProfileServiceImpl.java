package com.fittracker.user.service.implementation;

import com.fittracker.common.exceptions.ResourceNotFoundException;
import com.fittracker.user.persistence.entity.UserProfile;
import com.fittracker.user.persistence.repository.UserProfileRepository;
import com.fittracker.user.presentation.dto.CreateProfileRequest;
import com.fittracker.user.presentation.dto.UpdateProfileRequest;
import com.fittracker.user.presentation.dto.UserProfileResponse;
import com.fittracker.user.service.exception.ProfileAlreadyExistsException;
import com.fittracker.user.service.interfaces.UserProfileService;
import com.fittracker.user.utils.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository repository;
    private final UserProfileMapper mapper;

    @Override
    @Transactional
    public UserProfileResponse createProfile(CreateProfileRequest request) {
        if (repository.existsByUserId(request.userId())) {
            throw new ProfileAlreadyExistsException("Ya existe un perfil para el usuario: " + request.userId());
        }

        UserProfile entity = mapper.toEntity(request);
        UserProfile saved = repository.save(entity);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfileByUserId(UUID userId) {
        UserProfile entity = repository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado para usuario: " + userId));
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        UserProfile entity = repository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado para usuario: " + userId));

        mapper.updateEntity(entity, request);
        UserProfile updated = repository.save(entity);
        return mapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteProfile(UUID userId) {
        UserProfile entity = repository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado para usuario: " + userId));
        repository.delete(entity);
    }
}
