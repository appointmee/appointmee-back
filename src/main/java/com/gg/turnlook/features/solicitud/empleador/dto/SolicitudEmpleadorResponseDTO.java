package com.gg.turnlook.features.solicitud.empleador.dto;

import com.gg.turnlook.features.solicitud.EstadoSolicitud;
import com.gg.turnlook.features.usuario.dto.UsuarioResponseDTO;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;




@Data
@AllArgsConstructor
@NoArgsConstructor
public class SolicitudEmpleadorResponseDTO {


    private Integer id;
    private String motivo;
    private EstadoSolicitud estado;
    private LocalDate fechaSolicitud;
    private UsuarioResponseDTO usuario;


}




