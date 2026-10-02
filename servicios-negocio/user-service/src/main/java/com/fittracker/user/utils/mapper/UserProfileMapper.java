package com.fittracker.user.utils.mapper;

import com.fittracker.user.persistence.entity.UserProfile;
import com.fittracker.user.presentation.dto.CreateProfileRequest;
import com.fittracker.user.presentation.dto.UpdateProfileRequest;
import com.fittracker.user.presentation.dto.UserProfileResponse;
import org.springframework.stereotype.Component;

@Component
public class UserProfileMapper {

    public UserProfileResponse toResponse(UserProfile entity) {
        return new UserProfileResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getEmail(),
                entity.getName(),
                entity.getBodyWeight(),
                entity.getHeight(),
                entity.getDateOfBirth(),
                entity.getGender(),
                entity.getFitnessGoal(),
                entity.getExperienceLevel(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public UserProfile toEntity(CreateProfileRequest request) {
        return UserProfile.builder()
                .userId(request.userId())
                .email(request.email())
                .name(request.name())
                .bodyWeight(request.bodyWeight())
                .height(request.height())
                .dateOfBirth(request.dateOfBirth())
                .gender(request.gender())
                .fitnessGoal(request.fitnessGoal())
                .experienceLevel(request.experienceLevel())
                .build();
    }

    public void updateEntity(UserProfile entity, UpdateProfileRequest request) {
        if (request.name() != null) entity.setName(request.name());
        if (request.bodyWeight() != null) entity.setBodyWeight(request.bodyWeight());
        if (request.height() != null) entity.setHeight(request.height());
        if (request.dateOfBirth() != null) entity.setDateOfBirth(request.dateOfBirth());
        if (request.gender() != null) entity.setGender(request.gender());
        if (request.fitnessGoal() != null) entity.setFitnessGoal(request.fitnessGoal());
        if (request.experienceLevel() != null) entity.setExperienceLevel(request.experienceLevel());
    }
}
