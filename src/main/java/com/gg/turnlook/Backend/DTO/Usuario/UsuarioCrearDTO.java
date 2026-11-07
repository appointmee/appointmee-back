package com.gg.turnlook.Backend.DTO.Usuario;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class UsuarioCrearDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 60, message = "El nombre debe estar entre 3 y 60 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$", message = "El nombre solo puede contener letras")
    //                   lo q permite q haya
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 3, max = 60, message = "El apellido debe estar entre 3 y 60 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$", message = "El apellido solo puede contener letras")
    private String apellido;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 67, message = "La contraseña debe tener como minimo 8 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%&*!_-]).+$",
            message = "La contraseña debe contener al menos una minuscula, una mayuscula, " +
                    "un numero y un caracter especial (@#$%&*!_-)")
    private String password;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email es invalido")
    @Size(min = 8, max = 150, message = "El email debe estar entre 8 y 150 caracteres")
    private String email;

    @URL(message = "La foto de perfil debe ser una URL valida")
    private String fotoUrl;
}
