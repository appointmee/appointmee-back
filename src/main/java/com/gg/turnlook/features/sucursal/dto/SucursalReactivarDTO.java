package com.gg.turnlook.features.sucursal.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SucursalReactivarDTO {

    /// La baja libera el Place ID, asi que hace falta uno nuevo para reactivar.

    @NotBlank(message = "El Google Place ID es obligatorio para reactivar la sucursal")
    @Size(max = 255, message = "El Google Place ID no puede superar los 255 caracteres")
    private String googlePlaceId;

    /// Opcionales: si no se envian se conservan los valores que tenia la
    /// sucursal antes de darla de baja.

    @DecimalMin(value = "-90.0", message = "La latitud debe estar entre -90 y 90")
    @DecimalMax(value = "90.0", message = "La latitud debe estar entre -90 y 90")
    private Double latitud;

    @DecimalMin(value = "-180.0", message = "La longitud debe estar entre -180 y 180")
    @DecimalMax(value = "180.0", message = "La longitud debe estar entre -180 y 180")
    private Double longitud;

}
