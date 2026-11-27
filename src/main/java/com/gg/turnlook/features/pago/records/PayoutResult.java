package com.gg.turnlook.features.pago.records;

import com.gg.turnlook.features.pago.PayoutStatus;

import java.time.LocalDateTime;

/** Resultado de una operación de pago o reembolso. */
public record PayoutResult(
        String reference,
        PayoutStatus status,
        String providerReference,
        LocalDateTime processedAt) {
}
