package com.gg.turnlook.features.turno.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Proyeccion minima usada para calcular disponibilidad: fecha del turno y
 * duracion del servicio, sin cargar las entidades completas.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TurnoDisponibilidadDTO {

    private LocalDateTime fechaHora;
    private Integer duracionServicio;

}
