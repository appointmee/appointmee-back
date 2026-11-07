package com.gg.turnlook.Backend.Repository;



import com.gg.turnlook.Backend.Enum.EstadoSolicitud;
import com.gg.turnlook.Backend.Model.SolicitudEmpleado;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;



public interface SolicitudEmpleadoRepository extends JpaRepository<SolicitudEmpleado, Integer> {


    List<SolicitudEmpleado> findBySucursalId(Integer sucursalId);

    List<SolicitudEmpleado> findByEmpleadoEmail(String empleadoEmail);

    boolean existsByEmpleadoIdAndSucursalIdAndEstado(Integer empleadoId, Integer sucursalId,
                                                     EstadoSolicitud estadoSolicitud);

}
