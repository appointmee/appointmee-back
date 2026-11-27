package com.gg.turnlook.features.pago;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    List<PaymentTransaction> findByPagoIdOrderByIdAsc(Integer pagoId);

    boolean existsByPagoIdAndProviderTransactionId(Integer pagoId, String providerTransactionId);
}
