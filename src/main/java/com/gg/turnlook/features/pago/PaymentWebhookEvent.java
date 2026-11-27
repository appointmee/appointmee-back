package com.gg.turnlook.features.pago;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Evento recibido del proveedor. La clave (provider, event_id) garantiza idempotencia. */
@Entity
@Table(name = "payment_webhook_events", uniqueConstraints =
        @UniqueConstraint(name = "uk_webhook_event_proveedor", columnNames = {"provider", "event_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(name = "event_id", nullable = false, length = 150)
    private String eventId;

    @Column(length = 80)
    private String topic;

    @Column(length = 80)
    private String action;

    @Column(name = "payload_hash", length = 64)
    private String payloadHash;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @jakarta.persistence.PrePersist
    void alCrear() {
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }
    }
}
