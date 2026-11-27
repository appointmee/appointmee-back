package com.gg.turnlook.features.pago.records;

import com.gg.turnlook.features.pago.PaymentState;

/** Resultado de dominio de un cobro iniciado. */
public record CobroCreado(
        String proveedorReferencia,
        String initPoint,
        String externalReference,
        PaymentState estado) {
}
