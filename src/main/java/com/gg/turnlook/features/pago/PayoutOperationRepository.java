package com.gg.turnlook.features.pago;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayoutOperationRepository extends JpaRepository<PayoutOperation, Long> {

    Optional<PayoutOperation> findByIdempotencyKey(String idempotencyKey);

    Optional<PayoutOperation> findByReference(String reference);
}
