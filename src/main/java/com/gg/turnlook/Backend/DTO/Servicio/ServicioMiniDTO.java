package com.gg.turnlook.Backend.DTO.Servicio;




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
    private Integer puntuacion;
    private Long cantidadPuntuaciones;

}




