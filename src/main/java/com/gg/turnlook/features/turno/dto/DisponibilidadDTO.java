package com.gg.turnlook.features.turno.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@AllArgsConstructor
@NoArgsConstructor
public class DisponibilidadDTO {

    private LocalDate fecha;
    private List<LocalTime> horarios;
}
