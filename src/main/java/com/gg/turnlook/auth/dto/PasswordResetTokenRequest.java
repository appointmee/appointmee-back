package com.gg.turnlook.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetTokenRequest(@NotBlank @Size(max = 256) String token) {}
