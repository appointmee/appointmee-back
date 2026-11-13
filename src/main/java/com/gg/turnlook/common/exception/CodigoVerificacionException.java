package com.gg.turnlook.common.exception;

import com.gg.turnlook.auth.dto.EstadoVerificacionResponse;

public class CodigoVerificacionException extends BadRequestException {
    private final String code;
    private final EstadoVerificacionResponse estado;

    public CodigoVerificacionException(String code, EstadoVerificacionResponse estado) {
        super("EMAIL_ALREADY_VERIFIED".equals(code)
                ? "La cuenta ya está verificada"
                : "VERIFICATION_ATTEMPTS_EXCEEDED".equals(code)
                    ? "Alcanzaste el máximo de intentos. Pedí un código nuevo."
                    : "El código es incorrecto o venció");
        this.code = code;
        this.estado = estado;
    }

    public String getCode() { return code; }
    public EstadoVerificacionResponse getEstado() { return estado; }
}
