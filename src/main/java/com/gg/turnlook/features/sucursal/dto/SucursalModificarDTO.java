package com.gg.turnlook.features.sucursal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import java.time.LocalTime;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.util.List;

@Data
public class SucursalModificarDTO {


    @Size(min = 4, max = 120, message = "El nombre debe estar entre 4 y 120 caracteres")
    private String nombre;


    @Size(min = 10, max = 60, message = "La direccion debe estar entre 10 y 60 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\d\\s.,#-]+$",
            message = "La direccion solo debe contener letras, numeros y espacios")
    private String direccion;


    @Size(min = 8, max = 20, message = "El telefono debe estar entre los 8 y 20 digitos")
    @Pattern(regexp = "^\\d+$" , message = "El telefono solo puede contener numeros")
    private String telefono;


    // El max tiene que coincidir con el de SucursalCrearDTO (1500). Con 255 las
    // sucursales ya cargadas de mas de 255 caracteres no se podian volver a editar.
    @Size(min = 10, max = 1500, message = "La descripcion debe estar entre 10 y 1500 caracteres")
    private String descripcion;


    @Positive(message = "El ID de categoria debe ser positivo")
    private Integer categoriaId;


    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaApertura;


    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaCierre;


    @Size(max = 7, message = "No se pueden marcar mas de 7 dias de apertura")
    private List<String> diasApertura;


    @URL(message = "La foto de perfil debe ser una URL valida")
    private String fotoUrl;

    private String fotoPublicId;

}
