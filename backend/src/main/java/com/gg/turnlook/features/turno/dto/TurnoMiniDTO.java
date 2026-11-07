package com.gg.turnlook.features.turno.dto;

import com.gg.turnlook.features.turno.EstadoTurno;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;







@Data
@AllArgsConstructor
@NoArgsConstructor
public class TurnoMiniDTO {


    private Integer id;
    private String nombreServicio;
    private LocalDateTime fechaTurno;
    private EstadoTurno estadoTurno;
    private Integer puntuacion;

}


