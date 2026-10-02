package com.fittracker.auth.utils.mapper;

import com.fittracker.auth.persistence.entity.User;
import com.fittracker.auth.presentation.dto.AuthResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class AuthMapper {

    public AuthResponse toAuthResponse(User user, String token, List<String> roles) {
        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                roles
        );
    }
}
