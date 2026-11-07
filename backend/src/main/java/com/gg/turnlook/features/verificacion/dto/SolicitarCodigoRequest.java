package com.gg.turnlook.features.verificacion.dto;

import com.gg.turnlook.features.verificacion.enums.CanalVerificacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record SolicitarCodigoRequest(

        @NotNull CanalVerificacion canal,
        @NotBlank String destinatario
        ) {

}
