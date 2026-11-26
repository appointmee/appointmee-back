package com.gg.turnlook.features.notificacion.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Página de notificaciones con el contador de no leídas ya calculado. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginaNotificacionesDTO {

    private List<NotificacionDTO> contenido;
    private int pagina;
    private int tamano;
    private long totalElementos;
    private int totalPaginas;
    private long noLeidas;
}
