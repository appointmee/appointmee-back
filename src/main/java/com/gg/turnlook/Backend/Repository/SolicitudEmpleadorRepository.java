package com.gg.turnlook.Backend.Repository;


import com.gg.turnlook.Backend.Enum.EstadoSolicitud;
import com.gg.turnlook.Backend.Model.SolicitudEmpleador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface SolicitudEmpleadorRepository extends JpaRepository<SolicitudEmpleador, Integer> {


    boolean existsByUsuarioIdAndEstado(Integer userId, EstadoSolicitud estado);


    List<SolicitudEmpleador> findByEstado(EstadoSolicitud estado);


    List<SolicitudEmpleador> findByUsuarioEmail(String userEmail);


}



