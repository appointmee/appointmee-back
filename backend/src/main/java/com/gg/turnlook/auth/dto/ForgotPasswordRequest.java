package com.gg.turnlook.auth.dto;
import jakarta.validation.constraints.*;
public record ForgotPasswordRequest(@NotBlank @Email @Size(max = 150) String email) {}
