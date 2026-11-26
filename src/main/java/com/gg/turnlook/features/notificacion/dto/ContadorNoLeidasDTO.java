package com.gg.turnlook.features.notificacion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Contador de notificaciones sin leer del usuario autenticado. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContadorNoLeidasDTO {

    private long noLeidas;
}
