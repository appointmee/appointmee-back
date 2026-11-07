package com.gg.turnlook.Backend.Repository;



import com.gg.turnlook.Backend.Enum.EstadoTurno;
import com.gg.turnlook.Backend.Model.Turno;
import com.gg.turnlook.Backend.Model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;



public interface TurnoRepository extends JpaRepository<Turno, Integer> {


    List<Turno> findByEmpleadoAndFechaHoraBetweenAndEstadoNot(Usuario empleado,
                                                  LocalDateTime inicioDia, LocalDateTime finDia,
                                                              EstadoTurno estadoTurno);


    List<Turno> findByServicioSucursalIdAndEstado(Integer sucursalId, EstadoTurno estadoTurno);


    List<Turno> findByClienteEmail(String clienteEmail);


    List<Turno> findByEmpleadoEmailAndServicioSucursalIdAndEstado(
            String empleadoEmail, Integer sucursalId, EstadoTurno estadoTurno);


}


