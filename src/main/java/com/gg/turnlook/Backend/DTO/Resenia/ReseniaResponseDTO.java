package com.gg.turnlook.Backend.DTO.Resenia;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReseniaResponseDTO {


    private Integer puntuacion;
    private String comentario;
    private LocalDate fechaResenia;


}





