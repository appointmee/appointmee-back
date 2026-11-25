package com.gg.turnlook.features.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversacionCrearDTO {

    /** Identificador del turno. La sucursal y las partes se derivan de él. */
    @NotNull(message = "Falta el identificador del turno")
    private Integer turnoId;
}
