package com.gg.turnlook.features.pago;

/** Estado de la liquidación interna hacia la sucursal (opción C). */
public enum SettlementState {
    NOT_DUE,
    HELD_POLICY,
    RELEASE_PENDING,
    RELEASED,
    REFUND_PENDING,
    REFUNDED
}
