package com.gg.turnlook.Backend.DTO.SolicitudEmpleador;



import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class SolicitudEmpleadorCrearDTO {


    @NotBlank(message = "El motivo es obligatorio")
    @Size(min = 20, max = 300, message = "El motivo debe tener entre 20 y 300 caracteres")
    private String motivo;


}




