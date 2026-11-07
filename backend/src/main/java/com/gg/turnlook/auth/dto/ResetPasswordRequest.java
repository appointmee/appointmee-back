package com.gg.turnlook.auth.dto;
import jakarta.validation.constraints.*;
public record ResetPasswordRequest(@NotBlank @Size(max = 256) String token, @NotBlank @Size(min = 8, max = 67) @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%&*!_-]).+$") String newPassword) {}
