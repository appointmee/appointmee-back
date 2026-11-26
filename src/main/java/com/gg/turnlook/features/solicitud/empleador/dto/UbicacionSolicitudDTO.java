package com.gg.turnlook.features.solicitud.empleador.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Datos que el frontend obtiene de Google Places. No se consulta Places desde
 * el backend: solo se reciben y validan estos cuatro valores.
 */
@Data
public class UbicacionSolicitudDTO {

    @NotBlank(message = "La direccion de la ubicacion es obligatoria")
    @Size(max = 500, message = "La direccion no puede superar los 500 caracteres")
    private String direccion;

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
