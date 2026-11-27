package com.gg.turnlook.features.pago;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, Long> {

    boolean existsByProviderAndEventId(String provider, String eventId);

    Optional<PaymentWebhookEvent> findByProviderAndEventId(String provider, String eventId);
}
