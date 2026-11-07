package com.gg.turnlook.Backend.Repository;

import com.gg.turnlook.Backend.Enum.ERol;
import com.gg.turnlook.Backend.Model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol,Integer> {

    Optional<Rol> findByRol(ERol rol);
}
