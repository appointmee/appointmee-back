package com.gg.turnlook.features.turno.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Datos históricos de la sucursal asociada al turno.
 *
 * <p>Se derivan de la relación ya existente entre Turno, Servicio y Sucursal:
 * la baja lógica conserva la fila y estos datos siguen siendo identificables
 * aunque la sucursal ya no esté activa.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SucursalTurnoResponseDTO {

    private Integer id;
    private String nombre;
    private String direccion;
    private String telefono;
    private String categoria;
    private String fotoPerfil;
}
