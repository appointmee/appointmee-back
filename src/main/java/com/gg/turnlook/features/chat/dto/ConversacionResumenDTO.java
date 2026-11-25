package com.gg.turnlook.features.chat.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversacionResumenDTO {

    private Integer id;
    private Integer sucursalId;
    private String sucursalNombre;
    private Integer turnoId;
    private LocalDateTime fechaCreacion;
    private String ultimoMensaje;
    private LocalDateTime ultimoMensajeFecha;
    private long noLeidos;
    private String interlocutor;
    private String rol;
}
