package com.gg.turnlook.features.verificacion.dto;

import com.gg.turnlook.features.verificacion.enums.CanalVerificacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record VerificarCodigoRequest(
        @NotNull CanalVerificacion canal,
        @NotBlank String destinatario,
        @NotBlank
        @Pattern(regexp = "\\d{6}", message = "El código debe tener 6 dígitos")
        String codigo
) {}