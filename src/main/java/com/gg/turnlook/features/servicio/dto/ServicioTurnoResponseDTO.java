package com.gg.turnlook.features.servicio.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;




@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServicioTurnoResponseDTO {

    private Integer id;
    private String nombre;
    private BigDecimal precio;
    private Integer duracion;
}
