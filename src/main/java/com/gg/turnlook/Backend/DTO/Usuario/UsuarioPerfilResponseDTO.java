package com.gg.turnlook.Backend.DTO.Usuario;



import com.gg.turnlook.Backend.DTO.Sucursal.SucursalMiniDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;



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


