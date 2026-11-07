package com.gg.turnlook.Backend.DTO.SolicitudEmpleado;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class SolicitudEmpleadoMiniSucursalDTO {


    private Integer id;
    private String nombreEmpleado;
    private String apellidoEmpleado;
    private LocalDate fechaSolicitud;


}




