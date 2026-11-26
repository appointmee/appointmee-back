package com.gg.turnlook.features.solicitud.empleador.dto;

import com.gg.turnlook.features.solicitud.EstadoSolicitud;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;






@Data
@AllArgsConstructor
@NoArgsConstructor
public class SolicitudEmpleadorMiniDTO {


    private Integer id;
    private String nombreUsuario;
    private String apellidoUsuario;
    private LocalDate fechaSolicitud;

    /// Estado de la solicitud. Se agrega al contrato existente sin quitar
    /// ningun campo, para que el listado administrativo pueda mostrar
    /// pendientes y resueltas.
    private EstadoSolicitud estado;


}



