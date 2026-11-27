package com.gg.turnlook.features.mercadopago;

import com.gg.turnlook.features.payment.PaymentState;

/** Estados del proveedor traducidos al dominio. Ningún valor del SDK se expone hacia afuera. */
public enum EstadoPagoProveedor {
    APPROVED,
    REJECTED,
    PENDING,
    CHARGED_BACK,
    UNKNOWN;

    /** Traducción al estado financiero del dominio (no al estado del turno). */
    public PaymentState aPaymentState() {
        return switch (this) {
            case APPROVED -> PaymentState.CAPTURED;
            case REJECTED -> PaymentState.FAILED;
            case PENDING -> PaymentState.AUTHORIZED;
            case CHARGED_BACK -> PaymentState.CHARGED_BACK;
            case UNKNOWN -> PaymentState.CREATED;
        };
    }

    public static EstadoPagoProveedor desdeProveedor(String status, String statusDetail) {
        if (status == null || status.isBlank()) {
            return UNKNOWN;
        }
        return switch (status.toLowerCase()) {
            case "approved", "accredited", "processed" -> APPROVED;
            case "rejected", "failed", "cancelled", "canceled" -> REJECTED;
            case "charged_back" -> CHARGED_BACK;
            case "pending", "in_process", "action_required", "processing", "authorized" -> PENDING;
            default -> UNKNOWN;
        };
    }
}
