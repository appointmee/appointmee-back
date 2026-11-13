package com.gg.turnlook.features.usuario.dto;

import com.gg.turnlook.features.sucursal.dto.SucursalMiniDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;







@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioPerfilResponseDTO {


    private Integer id;
    private String nombre;
    private String apellido;
    private String email;
    private LocalDate fechaCreacion;
    private String fotoPerfil;
    private Set<String> roles;
    private List<SucursalMiniDTO> sucursalesEmpleado;


}


