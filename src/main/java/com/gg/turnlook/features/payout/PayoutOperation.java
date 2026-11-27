package com.gg.turnlook.features.payout;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Resultado persistido de una orden de pago simulada. */
@Entity
@Table(name = "payout_operations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayoutOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String reference;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "sucursal_id", nullable = false)
    private Integer sucursalId;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "provider_reference", nullable = false, length = 100)
    private String providerReference;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @Column(length = 100)
    private String reason;
}
