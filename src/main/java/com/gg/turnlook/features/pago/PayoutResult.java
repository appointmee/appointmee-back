package com.gg.turnlook.features.pago;

import java.time.LocalDateTime;

/** Resultado de una operación de pago o reembolso. */
public record PayoutResult(
        String reference,
        PayoutStatus status,
        String providerReference,
        LocalDateTime processedAt) {
}
