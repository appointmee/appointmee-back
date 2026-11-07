package com.gg.turnlook.features.solicitud.empleado.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;





@Data
@AllArgsConstructor
@NoArgsConstructor
public class SolicitudEmpleadoMiniPropiaDTO {


    private Integer id;
    private String nombreSucursal;
    private LocalDate fechaSolicitud;


}



