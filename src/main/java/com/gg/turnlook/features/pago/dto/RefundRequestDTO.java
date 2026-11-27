package com.gg.turnlook.features.pago.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Solicitud de reembolso total o parcial. */
public record RefundRequestDTO(
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank @Size(max = 100) String reason) {
}
