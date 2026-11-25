package com.gg.turnlook.features.sucursal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;






@Data
@AllArgsConstructor
@NoArgsConstructor
public class SucursalMiniDTO {


    private Integer id;
    private String nombre;
    private String categoria;
    private String fotoPerfil;
    private Double puntuacion;
    private Long cantidadPuntuaciones;
    private List<String> diasApertura;

    /// Pueden ser NULL en sucursales creadas antes de guardar la ubicacion.
    private Double latitud;
    private Double longitud;

}



