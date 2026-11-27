package com.gg.turnlook.features.payment;

/** Error de integración con el proveedor, ya traducido al dominio. */
public class PaymentProviderException extends RuntimeException {

    public PaymentProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
