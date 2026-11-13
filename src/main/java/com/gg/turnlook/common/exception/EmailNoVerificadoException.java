package com.gg.turnlook.common.exception;

public class EmailNoVerificadoException extends RuntimeException {
    private final String verificationToken;

    public EmailNoVerificadoException(String verificationToken) {
        super("Verificá tu email para iniciar sesión");
        this.verificationToken = verificationToken;
    }

    public String getVerificationToken() { return verificationToken; }
}
