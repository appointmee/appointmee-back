package com.gg.turnlook.features.chat.dto;

import com.gg.turnlook.features.chat.EstadoEntrega;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MensajeResponseDTO {

    private Long id;
    private Integer conversacionId;
    private Integer autorId;
    private String autorNombre;
    private String contenido;
    private LocalDateTime fechaCreacion;
    private EstadoEntrega estadoEntrega;
    private UUID clienteMensajeId;
}
