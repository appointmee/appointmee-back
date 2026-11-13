package com.gg.turnlook.auth.dto;
import jakarta.validation.constraints.*;
public record RefreshTokenRequest(@NotBlank @Size(max = 256) String refreshToken) {}
