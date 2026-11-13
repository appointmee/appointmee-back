package com.gg.turnlook.features.servicio.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;







@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServicioResponseDTO {


     private Integer id;
     private String nombre;
     private String descripcion;
     private Integer duracion;
     private BigDecimal precio;
     private String fotoPerfil;
     private Double puntuacion;
     private Long cantidadPuntuaciones;
     private String nombreSucursal;
     private String direccionSucursal;
     private String categoriaSucursal;


}





