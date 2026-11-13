package com.gg.turnlook.features.imagen.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;
import org.hibernate.validator.constraints.URL;




@Data
public class ImagenDTO {


    @NotEmpty(message = "Se deben ingresar URLs obligatoriamente")
    @Size(min = 1, max = 5, message = "Se deben subir entre 1 y 5 imagenes")
    private List<@NotBlank(message = "Las URLs no pueden estar incompletas")
    @URL(message = "Formato invalido de URL")
            String> urls;

}


