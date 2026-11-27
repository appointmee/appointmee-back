package com.gg.turnlook.features.pago;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentReleaseRepository extends JpaRepository<PaymentRelease, Long> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<PaymentRelease> findByIdempotencyKey(String idempotencyKey);

    List<PaymentRelease> findByPagoIdOrderByIdAsc(Integer pagoId);

    List<PaymentRelease> findByStatus(ReleaseStatus status);
}
