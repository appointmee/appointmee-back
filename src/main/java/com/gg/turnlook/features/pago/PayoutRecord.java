package com.gg.turnlook.features.pago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Representación del resultado persistido, sin depender de JPA. */
public record PayoutRecord(
        String reference,
        PayoutStatus status,
        BigDecimal amount,
        Integer sucursalId,
        String idempotencyKey,
        String providerReference,
        LocalDateTime processedAt,
        String reason) {
}
