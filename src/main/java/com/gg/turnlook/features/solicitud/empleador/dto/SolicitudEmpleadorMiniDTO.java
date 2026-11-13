package com.gg.turnlook.features.solicitud.empleador.dto;

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


}



