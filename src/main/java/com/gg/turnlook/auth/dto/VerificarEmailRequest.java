package com.gg.turnlook.auth.dto;

import jakarta.validation.constraints.*;

public record VerificarEmailRequest(
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Pattern(regexp = "[0-9]{6}", message = "El código debe tener 6 dígitos") String codigo
) {}
