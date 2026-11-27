package com.gg.turnlook.features.payment.comandos;

import java.math.BigDecimal;

/** Instrucción de reembolso de una orden de pago. */
public record RefundCommand(
        String payoutReference,
        BigDecimal amount,
        String reason,
        String idempotencyKey) {
}
