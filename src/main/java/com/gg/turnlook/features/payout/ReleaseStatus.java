package com.gg.turnlook.features.payout;

/** Estado de una orden de pago hacia la sucursal. */
public enum ReleaseStatus {
    PENDING,
    RELEASED,
    FAILED,
    CANCELED
}
