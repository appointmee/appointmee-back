package com.gg.turnlook.features.pago;

/** Estados del proveedor traducidos al dominio. Ningún valor del SDK se expone hacia afuera. */
public enum EstadoPagoProveedor {
    APPROVED,
    REJECTED,
    PENDING,
    CHARGED_BACK,
    UNKNOWN;

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
