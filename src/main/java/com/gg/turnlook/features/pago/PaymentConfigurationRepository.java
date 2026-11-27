package com.gg.turnlook.features.pago;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentConfigurationRepository extends JpaRepository<PaymentConfiguration, Integer> {

    Optional<PaymentConfiguration> findBySucursalIdAndProviderCode(Integer sucursalId, String providerCode);

    List<PaymentConfiguration> findBySucursalIdAndActiveTrue(Integer sucursalId);
}
