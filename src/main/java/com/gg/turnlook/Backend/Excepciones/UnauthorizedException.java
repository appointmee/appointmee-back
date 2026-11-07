package com.gg.turnlook.Backend.Excepciones;

public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
