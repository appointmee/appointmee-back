package com.gg.turnlook.features.pago.comandos;

import java.math.BigDecimal;

/** Instrucción de pago a la sucursal acreedora (sin exponer proveedor). */
public record PayoutCommand(
        BigDecimal amount,
        Integer sucursalId,
        String reference,
        String idempotencyKey) {
}
