package com.gg.turnlook.Backend.DTO.Sucursal;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.LocalTime;

@Data
public class SucursalModificarDTO {


    @Size(min = 4, max = 120, message = "El nombre debe estar entre 4 y 120 caracteres")
    private String nombre;


    @Size(min = 10, max = 80, message = "La direccion debe estar entre 10 y 80 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\d\\s.,#-]+$",
            message = "La direccion solo debe contener letras, numeros y espacios")
    private String direccion;


    @Size(min = 8, max = 20, message = "El telefono debe estar entre los 8 y 20 digitos")
    @Pattern(regexp = "^\\d+$" , message = "El telefono solo puede contener numeros")
    private String telefono;


    @Size(min = 10, max = 255, message = "La descripcion debe estar entre 10 y 255 caracteres")
    private String descripcion;


    @Positive(message = "El ID de categoria debe ser positivo")
    private Integer categoriaId;


    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaApertura;


    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaCierre;


    @URL(message = "La foto de perfil debe ser una URL valida")
    private String fotoUrl;

}
