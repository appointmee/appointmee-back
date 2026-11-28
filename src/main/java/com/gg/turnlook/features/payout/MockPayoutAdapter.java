package com.gg.turnlook.features.payout;

import com.gg.turnlook.features.payment.PayoutProvider;
import com.gg.turnlook.features.payment.comandos.PayoutCommand;
import com.gg.turnlook.features.payment.comandos.RefundCommand;
import com.gg.turnlook.features.payment.records.PayoutResult;


import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adapter de liquidación para el prototipo académico: persiste la orden y devuelve
 * el resultado como si la transferencia se hubiera realizado. No mueve dinero real.
 *
 * <p>Se activa por defecto; para desactivarlo basta con configurar
 * {@code app.pagos.payout.mock=false} al incorporar un proveedor real.</p>
 */
@Component
@ConditionalOnProperty(name = "app.pagos.payout.mock", havingValue = "true", matchIfMissing = true)
public class MockPayoutAdapter implements PayoutProvider {

    private final PayoutStore store;

    public MockPayoutAdapter(PayoutStore store) {
        this.store = store;
    }

    @Override
    public PayoutResult pay(PayoutCommand command) {
        return store.findByIdempotencyKey(command.idempotencyKey())
                .map(this::mapear)
                .orElseGet(() -> {
                    String referencia = command.reference() != null
                            ? command.reference()
                            : "MockPayout-" + UUID.randomUUID();
                    PayoutRecord guardado = store.save(new PayoutRecord(
                            referencia,
                            PayoutStatus.PAID,
                            command.amount(),
                            command.sucursalId(),
                            command.idempotencyKey(),
                            "MOCK-" + UUID.randomUUID(),
                            LocalDateTime.now(),
                            "Liquidación simulada"));
                    return mapear(guardado);
                });
    }

    @Override
    public PayoutStatus getStatus(String payoutReference) {
        return store.findByReference(payoutReference)
                .map(PayoutRecord::status)
                .orElse(PayoutStatus.PENDING);
    }

    @Override
    public PayoutResult refund(RefundCommand command) {
        return store.findByIdempotencyKey(command.idempotencyKey())
                .map(this::mapear)
                .orElseGet(() -> {
                    PayoutRecord original = store.findByReference(command.payoutReference()).orElse(null);
                    if (original == null) {
                        return new PayoutResult(command.payoutReference(), PayoutStatus.FAILED,
                                "MOCK-REEMBOLSO-SIN-ORDEN", LocalDateTime.now());
                    }
                    return mapear(store.save(new PayoutRecord(
                            original.reference(),
                            PayoutStatus.REFUNDED,
                            command.amount(),
                            original.sucursalId(),
                            command.idempotencyKey(),
                            "MOCK-REFUND-" + UUID.randomUUID(),
                            LocalDateTime.now(),
                            command.reason())));
                });
    }

    private PayoutResult mapear(PayoutRecord record) {
        return new PayoutResult(record.reference(), record.status(),
                record.providerReference(), record.processedAt());
    }
}
