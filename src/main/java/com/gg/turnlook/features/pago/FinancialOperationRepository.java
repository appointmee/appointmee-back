package com.gg.turnlook.features.pago;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialOperationRepository extends JpaRepository<FinancialOperation, Long> {

    List<FinancialOperation> findAllByPagoIdOrderByIdAsc(Integer pagoId);

    Optional<FinancialOperation> findTopByOrderByIdDesc();
}
