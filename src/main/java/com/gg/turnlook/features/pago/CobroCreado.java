package com.gg.turnlook.features.pago;

/** Resultado de dominio de un cobro iniciado. */
public record CobroCreado(
        String proveedorReferencia,
        String initPoint,
        String externalReference,
        PaymentState estado) {
}
