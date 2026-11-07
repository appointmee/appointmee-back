package com.gg.turnlook.Backend.DTO.Usuario;



import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;



@Data
public class UsuarioEmailDTO {


    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email es invalido")
    private String email;


}


