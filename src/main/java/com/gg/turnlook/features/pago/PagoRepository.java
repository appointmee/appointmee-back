package com.gg.turnlook.features.pago;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoRepository extends JpaRepository<Pago, Integer> {

    Optional<Pago> findByExternalReference(String externalReference);

    Optional<Pago> findByIdempotencyKey(String idempotencyKey);

    List<Pago> findByTurnoIdOrderByCreatedAtDesc(Integer turnoId);

    List<Pago> findBySucursalIdOrderByCreatedAtDesc(Integer sucursalId);

    boolean existsByExternalReference(String externalReference);
}
