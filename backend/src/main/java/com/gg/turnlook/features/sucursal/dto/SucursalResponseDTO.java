package com.gg.turnlook.features.sucursal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.gg.turnlook.features.imagen.dto.ImagenResponseDTO;
import com.gg.turnlook.features.usuario.dto.UsuarioMiniDTO;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@AllArgsConstructor
@NoArgsConstructor
public class SucursalResponseDTO {

     private Integer id;
     private String nombre;
     private String direccion;
     private String telefono;
     private String descripcion;
     private LocalDate fechaCreacion;
     private String categoria;

    /// Las horas se devuelven como "HH:mm" para que el <input type="time"> del
    /// formulario de edicion no mande "HH:mm:ss" de vuelta (Jackson 3 por defecto
    /// serializa LocalTime con segundos) y el PUT sea rechazado al deserializar.

    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaApertura;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaCierre;

     private List<String> diasApertura;
     private Double puntuacion;
     private Long cantidadPuntuaciones;
     private UsuarioMiniDTO empleador;
     private Set<UsuarioMiniDTO> empleados;
     private String fotoPerfil;
     private List<ImagenResponseDTO> imagenes;

}
