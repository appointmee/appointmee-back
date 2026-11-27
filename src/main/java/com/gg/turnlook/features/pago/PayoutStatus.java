package com.gg.turnlook.features.pago;

/** Estado de una orden de pago hacia la sucursal. */
public enum PayoutStatus {
    PENDING,
    PAID,
    FAILED,
    REFUNDED
}
