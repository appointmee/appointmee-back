package com.gg.turnlook.features.usuario.rol;

import com.gg.turnlook.common.exception.NotFoundException;
import org.springframework.stereotype.Service;





@Service
public class RolService {


    private final RolRepository rolRepo;


    public RolService(RolRepository rolRepo) {
        this.rolRepo = rolRepo;
    }



    /// METODOS


    public Rol listarPorRol(ERol rol){
        return rolRepo.findByRol(rol)
                .orElseThrow(() -> new NotFoundException("No se encontró el rol ingresado"));
    }
}
