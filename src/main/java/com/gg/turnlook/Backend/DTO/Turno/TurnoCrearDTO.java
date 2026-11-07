package com.gg.turnlook.Backend.DTO.Turno;



import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;



@Data
public class TurnoCrearDTO {



    @NotNull(message = "La fecha y hora son obligatorias")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime fechaHora;


    @NotNull(message = "El id del empleado es obligatorio")
    @Positive(message = "El id del empleado tiene que ser positivo")
    private Integer empleadoId;


    @NotNull(message = "El id del servicio es obligatorio")
    @Positive(message = "El id del servicio tiene que ser positivo")
    private Integer servicioId;


}



