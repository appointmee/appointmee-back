package com.gg.turnlook.features.pago;

import com.gg.turnlook.features.sucursal.Sucursal;
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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** Configuración de cobro por sucursal y proveedor. */
@Entity
@Table(name = "payment_configurations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Sucursal sucursal;

    @Column(name = "provider_code", nullable = false, length = 30)
    private String providerCode = "MERCADOPAGO";

    /** Métodos habilitados separados por coma, administrados desde el backend. */
    @Column(name = "enabled_methods", length = 500)
    private String enabledMethods = "credit_card,debit_card,account_money";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentEnvironment environment = PaymentEnvironment.TEST;

    @Column(nullable = false)
    private boolean active = true;
}
