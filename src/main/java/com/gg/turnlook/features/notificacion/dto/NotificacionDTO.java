package com.gg.turnlook.features.notificacion.dto;

import com.gg.turnlook.features.notificacion.TipoDestinoNotificacion;
import com.gg.turnlook.features.notificacion.TipoNotificacion;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Representación pública de una notificación: nunca expone al usuario dueño. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionDTO {

    private Integer id;
    private TipoNotificacion tipo;
    private String titulo;
    private String mensaje;
    private LocalDateTime fecha;
    private boolean leida;
    /** Destino concreto (turno, pago, solicitud o conversación); puede faltar. */
    private TipoDestinoNotificacion destinoTipo;
    private Integer destinoId;
    private Integer sucursalId;
}
