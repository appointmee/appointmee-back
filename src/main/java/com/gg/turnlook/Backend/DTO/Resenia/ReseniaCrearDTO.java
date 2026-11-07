package com.gg.turnlook.Backend.DTO.Resenia;



import jakarta.validation.constraints.*;
import lombok.Data;



@Data
public class ReseniaCrearDTO {


    @NotNull(message = "La puntuacion es obligatoria")
    @Min(value = 1, message = "La puntuacion minima es 1")
    @Max(value = 5, message = "La puntuacion maxima es 5")
    private Integer puntuacion;


    @Size(min = 8, max = 400,
            message = "El comentario debe estar entre los 8 y 400 caracteres o no incluir uno")
    private String comentario;


}





