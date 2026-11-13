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


}





