package com.gg.turnlook.Backend.DTO.SolicitudEmpleador;


import com.gg.turnlook.Backend.DTO.Usuario.UsuarioResponseDTO;
import com.gg.turnlook.Backend.Enum.EstadoSolicitud;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;


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




