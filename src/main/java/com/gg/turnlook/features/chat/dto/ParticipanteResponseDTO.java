package com.gg.turnlook.features.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParticipanteResponseDTO {

    private Integer usuarioId;
    private String nombre;
    private String apellido;
    private String rol;
}
