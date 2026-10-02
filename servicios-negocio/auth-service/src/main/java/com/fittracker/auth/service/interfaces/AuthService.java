package com.fittracker.auth.service.interfaces;

import com.fittracker.auth.presentation.dto.*;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    TokenValidationResponse validateToken(String token);

    AuthResponse refreshToken(String token);
}
