package com.gg.turnlook.features.pago;

import java.util.Optional;

/** Almacén de resultados de liquidación; desacopla el adapter de la persistencia. */
public interface PayoutStore {

    Optional<PayoutRecord> findByIdempotencyKey(String idempotencyKey);

    Optional<PayoutRecord> findByReference(String reference);

    PayoutRecord save(PayoutRecord record);
}
