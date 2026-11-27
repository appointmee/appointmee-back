package com.gg.turnlook.features.pago;

/** Tipos de asiento del ledger financiero. */
public enum FinancialOperationType {
    CREATED,
    APPROVED,
    DEBIT_REGISTERED,
    RELEASE_REQUESTED,
    RELEASED,
    REFUND_REQUESTED,
    REFUNDED,
    RELEASE_FAILED,
    CHARGED_BACK
}
