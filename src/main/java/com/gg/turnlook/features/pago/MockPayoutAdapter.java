package com.gg.turnlook.features.pago;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adapter de liquidación para el prototipo académico: registra la orden y devuelve
 * el resultado como si la transferencia se hubiera realizado. No mueve dinero real.
 *
 * <p>Se activa por defecto; para desactivarlo basta con configurar
 * {@code app.pagos.payout.mock=false} al incorporar un proveedor real.</p>
 */
@Component
@ConditionalOnProperty(name = "app.pagos.payout.mock", havingValue = "true", matchIfMissing = true)
public class MockPayoutAdapter implements PayoutProvider {

    private final Map<String, PayoutResult> resultadosPorIdempotencia = new ConcurrentHashMap<>();
    private final Map<String, PayoutResult> resultadosPorReferencia = new ConcurrentHashMap<>();

    @Override
    public PayoutResult pay(PayoutCommand command) {
        PayoutResult existente = resultadosPorIdempotencia.get(command.idempotencyKey());
        if (existente != null) {
            return existente;
        }

        String referencia = command.reference() != null
                ? command.reference()
                : "MockPayout-" + UUID.randomUUID();
        PayoutResult resultado = new PayoutResult(
                referencia,
                PayoutStatus.PAID,
                "MOCK-" + UUID.randomUUID(),
                LocalDateTime.now());

        resultadosPorIdempotencia.put(command.idempotencyKey(), resultado);
        resultadosPorReferencia.put(referencia, resultado);
        return resultado;
    }

    @Override
    public PayoutStatus getStatus(String payoutReference) {
        PayoutResult resultado = resultadosPorReferencia.get(payoutReference);
        return resultado == null ? PayoutStatus.PENDING : resultado.status();
    }

    @Override
    public PayoutResult refund(RefundCommand command) {
        PayoutResult existente = resultadosPorIdempotencia.get(command.idempotencyKey());
        if (existente != null) {
            return existente;
        }

        PayoutResult original = resultadosPorReferencia.get(command.payoutReference());
        if (original == null) {
            return new PayoutResult(command.payoutReference(), PayoutStatus.FAILED,
                    "MOCK-REEMBOLSO-SIN-ORDEN", LocalDateTime.now());
        }

        PayoutResult resultado = new PayoutResult(
                original.reference(),
                PayoutStatus.REFUNDED,
                "MOCK-REFUND-" + UUID.randomUUID(),
                LocalDateTime.now());
        resultadosPorIdempotencia.put(command.idempotencyKey(), resultado);
        resultadosPorReferencia.put(original.reference(), resultado);
        return resultado;
    }
}
