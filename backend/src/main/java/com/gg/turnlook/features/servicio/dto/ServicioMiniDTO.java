package com.gg.turnlook.features.servicio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;







@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServicioMiniDTO {


    private Integer id;
    private String nombre;
    private Integer duracion;
    private String fotoPerfil;
    private Double puntuacion;
    private Long cantidadPuntuaciones;

}




