package com.gg.turnlook.features.pago.dto;

import com.gg.turnlook.features.payment.PaymentState;
import com.gg.turnlook.features.payment.SettlementState;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Contrato público de un cobro centralizado. No expone secretos del proveedor. */
public record PagoResponseDTO(
        Integer id,
        Integer turnoId,
        LocalDateTime fechaTurno,
        String nombreServicio,
        Integer sucursalId,
        String nombreSucursal,
        BigDecimal monto,
        String moneda,
        PaymentState paymentState,
        SettlementState settlementState,
        String providerPaymentId,
        String externalReference,
        LocalDateTime createdAt,
        LocalDateTime capturedAt) {
}
