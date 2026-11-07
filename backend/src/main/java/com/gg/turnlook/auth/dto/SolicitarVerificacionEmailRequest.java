package com.gg.turnlook.auth.dto;
import jakarta.validation.constraints.*;

public record SolicitarVerificacionEmailRequest(
        @NotBlank @Email @Size(max = 150) String email,
        Boolean reenviar
) {}
