package com.gg.turnlook.Backend.DTO.Turno;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DisponibilidadDTO {

    private LocalDate fecha;
    private List<LocalTime> horarios;
}
