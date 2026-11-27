package com.gg.turnlook.features.resenia.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;





@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReseniaResponseDTO {


    private Integer puntuacion;
    private String comentario;
    private LocalDate fechaResenia;

    /** Datos historicos; null cuando el cliente no pidio incluir al empleado. */
    private String empleadoNombre;
    private String empleadoApellido;

    /** false si no se incluyo al empleado o su cuenta ya no esta activa en la sucursal. */
    private Boolean empleadoPerteneceASucursal;


    /** Constructor anterior: resenias sin empleado y detalles de turno. */
    public ReseniaResponseDTO(Integer puntuacion, String comentario, LocalDate fechaResenia) {
        this(puntuacion, comentario, fechaResenia, null, null, false);
    }


}





