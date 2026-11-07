package com.gg.turnlook.Backend.DTO.Sucursal;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.LocalTime;

@Data
public class SucursalCrearDTO {


    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 4, max = 120, message = "El nombre debe estar entre 4 y 120 caracteres")
    private String nombre;

    @NotBlank(message = "La direccion es obligatoria")
    @Size(min = 10, max = 60, message = "La direccion debe estar entre 10 y 60 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\d\\s.,#-]+$",
             message = "La direccion solo debe contener letras y numeros")
    private String direccion;

    @Size(min = 8, max = 20, message = "El telefono debe estar entre los 8 y 20 digitos")
    @Pattern(regexp = "^\\d+$" , message = "El telefono solo puede contener numeros")
    private String telefono;

    @NotBlank(message = "La descripcion es obligatoria")
    @Size(min = 10, max = 1500, message = "La descripcion debe estar entre 10 y 1500 caracteres")
    private String descripcion;

    @NotNull(message = "El ID de categoria es obligatorio")
    @Positive(message = "El ID de categoria debe ser positivo")
    private Integer categoriaId;

    @NotNull(message = "La hora de apertura es obligatoria")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaApertura;

    @NotNull(message = "La hora de cierre es obligatoria")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaCierre;

    @NotBlank(message = "La foto de perfil es obligatoria")
    @URL(message = "La foto de perfil debe ser una URL valida")
    private String fotoUrl;

}
