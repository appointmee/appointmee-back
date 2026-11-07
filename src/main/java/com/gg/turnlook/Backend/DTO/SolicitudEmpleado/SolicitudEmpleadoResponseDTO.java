package com.gg.turnlook.Backend.DTO.SolicitudEmpleado;



import com.gg.turnlook.Backend.DTO.Sucursal.SucursalMiniDTO;
import com.gg.turnlook.Backend.DTO.Usuario.UsuarioResponseDTO;
import com.gg.turnlook.Backend.Enum.EstadoSolicitud;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


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




