package com.gg.turnlook.features.pago.comandos;

import java.math.BigDecimal;

/** Comando de dominio para iniciar un cobro. No contiene tipos del proveedor. */
public record CrearCobroCommand(
        Integer pagoId,
        BigDecimal monto,
        String moneda,
        String descripcion,
        String emailPagador,
        String idempotencyKey) {
}
