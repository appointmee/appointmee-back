package com.gg.turnlook.features.turno.dto;

import com.gg.turnlook.features.resenia.dto.ReseniaResponseDTO;
import com.gg.turnlook.features.servicio.dto.ServicioTurnoResponseDTO;
import com.gg.turnlook.features.turno.EstadoTurno;
import com.gg.turnlook.features.usuario.dto.UsuarioMiniDTO;
import com.gg.turnlook.features.usuario.dto.UsuarioResponseDTO;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;




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

    /** Datos históricos de la sucursal, disponibles aunque esté dada de baja. */
    private SucursalTurnoResponseDTO sucursal;
    /** Estado actual del empleado asignado, para distinguir una baja lógica. */
    private boolean empleadoActivo;
    /** Pertenencia actual del empleado a la sucursal del turno. */
    private boolean empleadoPerteneceASucursal;
    /** Estado actual de la sucursal del turno. */
    private boolean sucursalActiva;

}
