package com.gg.turnlook.Backend.DTO.SolicitudEmpleador;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;



@Data
@AllArgsConstructor
@NoArgsConstructor
public class SolicitudEmpleadorMiniDTO {


    private Integer id;
    private String nombreUsuario;
    private String apellidoUsuario;
    private LocalDate fechaSolicitud;


}



