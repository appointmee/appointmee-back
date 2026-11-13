package com.gg.turnlook.features.usuario.dto;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;







@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioMeDTO {


    private Integer id;
    private Set<String> roles;
    private String fotoPerfil;


}



