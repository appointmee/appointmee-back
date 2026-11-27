package com.gg.turnlook.features.resenia.dto;

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

    /**
     * Opcional: cuando es true se copia el empleado del turno a la resenia.
     * Por defecto es false para no alterar las resenas anteriores ni los payloads
     * que todavia no envian el campo.
     */
    private boolean incluirEmpleado = false;


}





