package com.gg.turnlook.Backend.DTO.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDTO {

    /// ATRIBUTOS

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email es invalido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    private String pass;

    /// CONSTRUCTORES
    public LoginDTO(String email, String pass) {
        this.email = email;
        this.pass = pass;
    }

    public LoginDTO() {
    }


}
