package com.gg.turnlook.Backend.DTO.Servicio;



import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;



@Data
public class ServicioModificarDTO {


    @Size(min = 4, max = 60, message = "El nombre debe estar entre 4 y 60 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s,-]+$",
            message = "El nombre solo puede tener letras y espacios")
    private String nombre;


    @Size(min = 10, max = 500, message = "La descripcion debe estar entre 10 y 500 caracteres")
    private String descripcion;


    @Min(value = 30, message = "La duracion minima es de 30 minutos")
    @Max(value = 360, message = "La duracion maxima es de 360 minutos (6 horas)")
    private Integer duracion;


    @DecimalMin(value = "0.01", message = "El precio minimo es 0.01")
    @DecimalMax(value = "999999.99", message = "El precio maximo es 999999.99")
    @Digits(integer = 6, fraction = 2,
            message = "El precio debe tener a lo sumo 6 enteros y 2 decimales")
    private BigDecimal precio;


    @URL(message = "La foto de perfil debe ser una URL valida")
    private String fotoUrl;

}
