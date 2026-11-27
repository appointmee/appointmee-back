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
import jakarta.persistence.PreRemove;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.Immutable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Asiento append-only del ledger. Cada fila enlaza con la anterior mediante
 * prev_hash/hash para detectar manipulaciones dentro del prototipo.
 */
@Entity
@Immutable
@Table(name = "financial_operations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pago_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Pago pago;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FinancialOperationType type;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "from_state", length = 30)
    private String fromState;

    @Column(name = "to_state", length = 30)
    private String toState;

    @Column(name = "provider_reference", length = 100)
    private String providerReference;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "prev_hash", length = 64)
    private String prevHash;

    @Column(name = "hash", length = 64)
    private String hash;

    @Column(length = 100)
    private String actor;

    @jakarta.persistence.PrePersist
    void alCrear() {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }

    /** El ledger es append-only: ningún código puede borrar un asiento. */
    @PreRemove
    void impedirBorrado() {
        throw new IllegalStateException("FinancialOperation es append-only");
    }
}
