package com.gg.turnlook.features.pago;

import com.gg.turnlook.features.sucursal.Sucursal;
import com.gg.turnlook.features.turno.Turno;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Cobro centralizado (opción C). El dinero se recibe en la cuenta de AppointMee y
 * la sucursal queda registrada como acreedora hasta la liquidación posterior.
 */
@Entity
@Table(name = "pagos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turno_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Turno turno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Sucursal sucursal;

    /** Identificador de correlación enviado al proveedor y usado en el webhook. */
    @Column(name = "external_reference", nullable = false, unique = true, length = 100)
    private String externalReference;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider = "MERCADOPAGO";

    @Column(name = "provider_payment_id", length = 100)
    private String providerPaymentId;

    @Column(name = "provider_status", length = 50)
    private String providerStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_state", nullable = false, length = 30)
    private PaymentState paymentState = PaymentState.CREATED;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_state", nullable = false, length = 30)
    private SettlementState settlementState = SettlementState.NOT_DUE;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "ARS";

    /** Clave enviada al proveedor en cada escritura; evita cobros duplicados. */
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "checkout_url", length = 1000)
    private String checkoutUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "captured_at")
    private LocalDateTime capturedAt;

    @PrePersist
    void alCrear() {
        LocalDateTime ahora = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = ahora;
        }
        updatedAt = ahora;
        // La sucursal acreedora se deriva del servicio del turno cuando no se informa.
        if (sucursal == null && turno != null && turno.getServicio() != null) {
            sucursal = turno.getServicio().getSucursal();
        }
    }

    @PreUpdate
    void alActualizar() {
        updatedAt = LocalDateTime.now();
    }
}
