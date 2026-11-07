package com.gg.turnlook.Backend.DTO.Sucursal;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@AllArgsConstructor
@NoArgsConstructor
public class SucursalMiniDTO {


    private Integer id;
    private String nombre;
    private String categoria;
    private String fotoPerfil;
    private Integer puntuacion;
    private Long cantidadPuntuaciones;

}



