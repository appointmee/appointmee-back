package com.gg.turnlook.features.ledger;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio del ledger. La entidad es {@code @Immutable} y tiene un @PreRemove
 * que bloquea borrados: en la práctica es append-only.
 */
public interface FinancialOperationRepository extends JpaRepository<FinancialOperation, Long> {

    List<FinancialOperation> findAllByPagoIdOrderByIdAsc(Integer pagoId);

    Optional<FinancialOperation> findTopByOrderByIdDesc();
}
