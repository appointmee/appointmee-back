package com.gg.turnlook.features.usuario.rol;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;



public interface RolRepository extends JpaRepository<Rol,Integer> {

    Optional<Rol> findByRol(ERol rol);
}
