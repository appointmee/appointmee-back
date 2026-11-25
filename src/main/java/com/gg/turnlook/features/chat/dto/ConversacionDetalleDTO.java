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
    /** Nombre del servicio del turno asociado, si lo hay. */
    private String servicioNombre;
    /** Cliente que solicitó el turno (participante del chat). */
    private String clienteNombre;
    /** Empleado que realiza el servicio; no participa del chat. */
    private String profesionalNombre;
    private LocalDateTime fechaCreacion;
    private List<ParticipanteResponseDTO> participantes;
}
