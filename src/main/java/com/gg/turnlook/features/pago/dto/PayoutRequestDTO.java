package com.gg.turnlook.features.pago.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Solicitud de liquidación hacia la sucursal. */
public record PayoutRequestDTO(
        @NotBlank @Size(max = 100) String reason) {
}
