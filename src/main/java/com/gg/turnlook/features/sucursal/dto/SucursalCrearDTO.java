package com.gg.turnlook.features.sucursal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import java.time.LocalTime;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.util.List;

@Data
public class SucursalCrearDTO {


    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 4, max = 120, message = "El nombre debe estar entre 4 y 120 caracteres")
    private String nombre;

    @NotBlank(message = "La direccion es obligatoria")
    @Size(min = 10, max = 255, message = "La direccion debe estar entre 10 y 255 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚüÜñÑªº\\d\\s.,#/()'&·°-]+$",
             message = "La direccion solo debe contener letras, numeros y caracteres habituales de direcciones")
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

    @NotEmpty(message = "Los dias de apertura son obligatorios")
    @Size(max = 7, message = "No se pueden marcar mas de 7 dias de apertura")
    private List<String> diasApertura;

    @NotBlank(message = "La foto de perfil es obligatoria")
    @URL(message = "La foto de perfil debe ser una URL valida")
    private String fotoUrl;

    private String fotoPublicId;


    /// La ubicacion es obligatoria para toda sucursal nueva: los tres valores
    /// viajan juntos y se validan aqui, antes de llegar al servicio.

    @NotNull(message = "La latitud es obligatoria")
    @DecimalMin(value = "-90.0", message = "La latitud debe estar entre -90 y 90")
    @DecimalMax(value = "90.0", message = "La latitud debe estar entre -90 y 90")
    private Double latitud;

    @NotNull(message = "La longitud es obligatoria")
    @DecimalMin(value = "-180.0", message = "La longitud debe estar entre -180 y 180")
    @DecimalMax(value = "180.0", message = "La longitud debe estar entre -180 y 180")
    private Double longitud;

    @NotBlank(message = "El Google Place ID es obligatorio")
    @Size(max = 255, message = "El Google Place ID no puede superar los 255 caracteres")
    private String googlePlaceId;

}
