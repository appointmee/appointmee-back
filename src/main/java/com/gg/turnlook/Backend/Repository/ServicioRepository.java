package com.gg.turnlook.Backend.Repository;

import com.gg.turnlook.Backend.Model.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServicioRepository extends JpaRepository<Servicio, Integer> {

    List<Servicio> findByActivoTrue();
    List<Servicio> findBySucursalId(Integer sucursalId);
}
