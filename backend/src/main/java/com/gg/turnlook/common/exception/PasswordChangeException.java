package com.gg.turnlook.common.exception;

public class PasswordChangeException extends BadRequestException {
    private final String code;

    public PasswordChangeException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() { return code; }
}
