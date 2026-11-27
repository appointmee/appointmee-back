package com.gg.turnlook.features.payment;

/** Estado del dinero en el proveedor. Independiente del estado del turno. */
public enum PaymentState {
    CREATED,
    AUTHORIZED,
    CAPTURED,
    FAILED,
    CANCELED,
    REFUNDED,
    CHARGED_BACK
}
