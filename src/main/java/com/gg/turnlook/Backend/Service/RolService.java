package com.gg.turnlook.Backend.Service;


import com.gg.turnlook.Backend.Enum.ERol;
import com.gg.turnlook.Backend.Excepciones.NotFoundException;
import com.gg.turnlook.Backend.Model.Rol;
import com.gg.turnlook.Backend.Repository.RolRepository;
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
