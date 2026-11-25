package com.gg.turnlook.features.chat.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversacionDetalleDTO {

    private Integer id;
    private Integer sucursalId;
    private String sucursalNombre;
    private Integer turnoId;
    private LocalDateTime fechaCreacion;
    private List<ParticipanteResponseDTO> participantes;
}
