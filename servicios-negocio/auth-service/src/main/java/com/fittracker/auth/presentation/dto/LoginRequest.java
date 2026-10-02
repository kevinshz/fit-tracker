package com.fittracker.auth.presentation.dto;

public record LoginRequest(
    String email,
    String password
) {}
