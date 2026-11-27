package com.gg.turnlook.features.sucursal.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Pagina del listado general de sucursales, con metadatos de Spring Data. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginaSucursalesDTO {

    private List<SucursalMiniDTO> content;
    private long totalElements;
    private int totalPages;
    private int number;
    private int size;

}
