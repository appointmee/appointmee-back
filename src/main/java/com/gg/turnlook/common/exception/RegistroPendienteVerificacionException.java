package com.gg.turnlook.common.exception;

public class RegistroPendienteVerificacionException extends ConflictException {
    public RegistroPendienteVerificacionException() {
        super("Ya existe una cuenta pendiente de verificación con ese email. Iniciá sesión para verificarla o restablecé tu contraseña si no la recordás.");
    }
}
