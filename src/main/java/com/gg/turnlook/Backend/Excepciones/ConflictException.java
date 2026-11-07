package com.gg.turnlook.Backend.Excepciones;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
