package com.gg.turnlook.Backend.DTO.Usuario;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioResponseDTO {


    private Integer id;
    private String nombre;
    private String apellido;
    private String email;
    private String urlFotoPerfil;

}


