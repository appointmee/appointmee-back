package com.gg.turnlook.Backend.DTO.Servicio;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServicioTurnoResponseDTO {

    private Integer id;
    private String nombre;
    private BigDecimal precio;
    private Integer duracion;
}
