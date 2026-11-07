package com.gg.turnlook.Backend.DTO.Usuario;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;



@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioMeDTO {


    private Integer id;
    private Set<String> roles;
    private String fotoPerfil;


}



