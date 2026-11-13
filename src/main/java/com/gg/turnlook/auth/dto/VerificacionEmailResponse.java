package com.gg.turnlook.auth.dto;

import com.gg.turnlook.features.usuario.dto.UsuarioMeDTO;

public record VerificacionEmailResponse(
        String message,
        String token,
        String accessToken,
        String refreshToken,
        long expiresIn,
        UsuarioMeDTO usuario
) {}
