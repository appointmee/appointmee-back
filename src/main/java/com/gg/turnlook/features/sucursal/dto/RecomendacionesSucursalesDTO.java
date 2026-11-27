package com.gg.turnlook.features.sucursal.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Secciones horizontales del Home. Las listas personales llegan vacias
 * cuando no hay usuario autenticado o no hay datos suficientes. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecomendacionesSucursalesDTO {

    private List<SucursalMiniDTO> mejores;
    private List<SucursalMiniDTO> masDemandadas;
    private List<SucursalMiniDTO> nuevas;
    private List<SucursalMiniDTO> vistasRecientes;
    private List<SucursalMiniDTO> paraVos;
    /** Vista previa del listado general para el acceso a la pagina completa. */
    private List<SucursalMiniDTO> todas;

}
