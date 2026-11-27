package com.gg.turnlook.features.pago;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** Orden de pago (simulada en la tesis) hacia la sucursal acreedora. */
@Entity
@Table(name = "payment_releases")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRelease {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pago_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Pago pago;

    @Column(name = "turno_id", nullable = false)
    private Integer turnoId;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(length = 100)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReleaseStatus status = ReleaseStatus.PENDING;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "provider_reference", length = 100)
    private String providerReference;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @jakarta.persistence.PrePersist
    void alCrear() {
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
    }
}
