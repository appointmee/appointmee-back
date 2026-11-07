package com.gg.turnlook.Backend.Excepciones;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
