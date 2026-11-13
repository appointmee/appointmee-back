package com.gg.turnlook.features.servicio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServicioMiniDTO {


    private Integer id;
    private String nombre;
    private Integer duracion;
    private BigDecimal precio;
    private String descripcion;
    private String fotoPerfil;
    private Double puntuacion;
    private Long cantidadPuntuaciones;

}




