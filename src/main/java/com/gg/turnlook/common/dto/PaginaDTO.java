package com.gg.turnlook.common.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Contrato comun de paginacion: contenido y metadatos de Spring Data. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginaDTO<T> {

    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int number;
    private int size;

}
