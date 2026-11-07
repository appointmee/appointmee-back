package com.gg.turnlook.features.solicitud.empleado.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;





@Data
@AllArgsConstructor
@NoArgsConstructor
public class SolicitudEmpleadoMiniSucursalDTO {


    private Integer id;
    private String nombreEmpleado;
    private String apellidoEmpleado;
    private LocalDate fechaSolicitud;


}




