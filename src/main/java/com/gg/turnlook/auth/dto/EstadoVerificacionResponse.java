package com.gg.turnlook.auth.dto;

public record EstadoVerificacionResponse(
        boolean verificada, boolean enviado, boolean enviadoAhora,
        long retryAfterSeconds, int attemptsRemaining, long expiresInSeconds
) {}
