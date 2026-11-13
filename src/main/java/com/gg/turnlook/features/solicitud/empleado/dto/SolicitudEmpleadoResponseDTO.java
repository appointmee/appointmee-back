package com.gg.turnlook.features.solicitud.empleado.dto;

import com.gg.turnlook.features.solicitud.EstadoSolicitud;
import com.gg.turnlook.features.sucursal.dto.SucursalMiniDTO;
import com.gg.turnlook.features.usuario.dto.UsuarioResponseDTO;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;






@Data
@AllArgsConstructor
@NoArgsConstructor
public class SolicitudEmpleadoResponseDTO {


    private Integer id;
    private EstadoSolicitud estadoSolicitud;
    private LocalDate fechaSolicitud;
    private UsuarioResponseDTO empleado;
    private SucursalMiniDTO sucursal;


}




