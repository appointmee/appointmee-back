package com.gg.turnlook.features.pago;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Puerto de liquidación. En la tesis se implementa con un adapter simulado;
 * el dominio no sabe si el dinero se mueve realmente o no.
 */
public interface PayoutProvider {

    PayoutResult pay(PayoutCommand command);

    PayoutStatus getStatus(String payoutReference);

    PayoutResult refund(RefundCommand command);

    /** Firma directa del contrato; delega en el comando para no duplicar lógica. */
    default PayoutResult pay(BigDecimal amount, Integer sucursalId, String reference,
                             String idempotencyKey) {
        return pay(new PayoutCommand(amount, sucursalId, reference, idempotencyKey));
    }

    /** Firma directa del contrato; genera una clave de idempotencia nueva. */
    default PayoutResult refund(String reference, BigDecimal amount) {
        return refund(new RefundCommand(reference, amount, "Reembolso solicitado",
                UUID.randomUUID().toString()));
    }
}
