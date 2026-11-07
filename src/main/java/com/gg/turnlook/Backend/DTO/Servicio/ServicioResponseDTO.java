package com.gg.turnlook.Backend.DTO.Servicio;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;



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
     private Integer puntuacion;
     private Long cantidadPuntuaciones;
     private String nombreSucursal;
     private String direccionSucursal;
     private String categoriaSucursal;


}





