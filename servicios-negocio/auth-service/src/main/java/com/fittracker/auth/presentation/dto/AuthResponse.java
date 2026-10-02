package com.fittracker.auth.presentation.dto;

import java.util.List;
import java.util.UUID;

public record AuthResponse(
    String token,
    UUID userId,
    String email,
    List<String> roles
) {}
