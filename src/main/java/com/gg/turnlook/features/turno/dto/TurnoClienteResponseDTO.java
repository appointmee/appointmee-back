package com.gg.turnlook.features.turno.dto;

import com.gg.turnlook.features.resenia.dto.ReseniaResponseDTO;
import com.gg.turnlook.features.servicio.dto.ServicioTurnoResponseDTO;
import com.gg.turnlook.features.turno.EstadoTurno;
import com.gg.turnlook.features.usuario.dto.UsuarioMiniDTO;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;






@Data
@AllArgsConstructor
@NoArgsConstructor
public class TurnoClienteResponseDTO {


    private LocalDateTime fechaReserva;
    private LocalDateTime fechaHora;
    private EstadoTurno estadoTurno;
    private UsuarioMiniDTO empleado;
    private ServicioTurnoResponseDTO servicio;
    private UsuarioMiniDTO cliente;
    private ReseniaResponseDTO resenia;


}





