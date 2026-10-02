package com.fittracker.user.service.interfaces;

import com.fittracker.user.presentation.dto.CreateProfileRequest;
import com.fittracker.user.presentation.dto.UpdateProfileRequest;
import com.fittracker.user.presentation.dto.UserProfileResponse;

import java.util.UUID;

public interface UserProfileService {

    UserProfileResponse createProfile(CreateProfileRequest request);

    UserProfileResponse getProfileByUserId(UUID userId);

    UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request);

    void deleteProfile(UUID userId);
}
