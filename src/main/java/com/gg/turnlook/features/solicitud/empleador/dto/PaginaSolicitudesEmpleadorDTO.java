package com.gg.turnlook.features.solicitud.empleador.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pagina de solicitudes de empleador para el listado administrativo.
 *
 * Los nombres de los campos siguen el contrato de paginacion de Spring Data
 * (content, totalElements, totalPages, number, size) para que el frontend
 * pueda construir la navegacion sin calculos adicionales.
 * Los elementos conservan todos los campos de SolicitudEmpleadorMiniDTO,
 * incluido el estado.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginaSolicitudesEmpleadorDTO {

    private List<SolicitudEmpleadorMiniDTO> content;
    private long totalElements;
    private int totalPages;
    private int number;
    private int size;

}
