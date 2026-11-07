package com.gg.turnlook.Backend.DTO.Imagen;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;
import java.util.List;


@Data
public class ImagenDTO {


    @NotEmpty(message = "Se deben ingresar URLs obligatoriamente")
    @Size(min = 1, max = 5, message = "Se deben subir entre 1 y 5 imagenes")
    private List<@NotBlank(message = "Las URLs no pueden estar incompletas")
    @URL(message = "Formato invalido de URL")
            String> urls;

}


