package com.gg.turnlook.common.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Contenedor inicial de un listado: solo los primeros registros y si hay mas. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VistaPreviaDTO<T> {

    private List<T> content;
    private long totalElements;
    private boolean hayMas;

}
