package com.gg.turnlook.features.payout;

import java.util.Optional;
import org.springframework.stereotype.Component;

/** Persiste los resultados de liquidación en PostgreSQL (o H2 en tests). */
@Component
public class JpaPayoutStore implements PayoutStore {

    private final PayoutOperationRepository repository;

    public JpaPayoutStore(PayoutOperationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PayoutRecord> findByIdempotencyKey(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey).map(this::mapear);
    }

    @Override
    public Optional<PayoutRecord> findByReference(String reference) {
        return repository.findByReference(reference).map(this::mapear);
    }

    @Override
    public PayoutRecord save(PayoutRecord record) {
        PayoutOperation operacion = new PayoutOperation();
        operacion.setReference(record.reference());
        operacion.setStatus(record.status().name());
        operacion.setAmount(record.amount());
        operacion.setSucursalId(record.sucursalId());
        operacion.setIdempotencyKey(record.idempotencyKey());
        operacion.setProviderReference(record.providerReference());
        operacion.setProcessedAt(record.processedAt());
        operacion.setReason(record.reason());
        return mapear(repository.saveAndFlush(operacion));
    }

    private PayoutRecord mapear(PayoutOperation operacion) {
        return new PayoutRecord(
                operacion.getReference(),
                PayoutStatus.valueOf(operacion.getStatus()),
                operacion.getAmount(),
                operacion.getSucursalId(),
                operacion.getIdempotencyKey(),
                operacion.getProviderReference(),
                operacion.getProcessedAt(),
                operacion.getReason());
    }
}
