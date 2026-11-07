package com.gg.turnlook.Backend.DTO.Turno;


import com.gg.turnlook.Backend.DTO.Resenia.ReseniaResponseDTO;
import com.gg.turnlook.Backend.DTO.Servicio.ServicioTurnoResponseDTO;
import com.gg.turnlook.Backend.DTO.Usuario.UsuarioResponseDTO;
import com.gg.turnlook.Backend.DTO.Usuario.UsuarioMiniDTO;
import com.gg.turnlook.Backend.Enum.EstadoTurno;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TurnoResponseDTO {

    private Integer id;
    private LocalDateTime fechaReserva;
    private LocalDateTime fechaTurno;
    private EstadoTurno estadoTurno;
    private UsuarioMiniDTO cliente;
    private UsuarioResponseDTO empleado;
    private ServicioTurnoResponseDTO servicio;
    private ReseniaResponseDTO resenia;

}
