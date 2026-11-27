package com.gg.turnlook.features.ledger;

/** Tipos de asiento del ledger financiero. */
public enum FinancialOperationType {
    CREATED,
    PAYMENT_FAILED,
    APPROVED,
    DEBIT_REGISTERED,
    RELEASE_REQUESTED,
    RELEASED,
    REFUND_REQUESTED,
    REFUNDED,
    RELEASE_FAILED,
    CHARGED_BACK
}
