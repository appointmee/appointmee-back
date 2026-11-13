package com.gg.turnlook.features.solicitud.empleado;

import com.gg.turnlook.features.solicitud.EstadoSolicitud;
import com.gg.turnlook.features.solicitud.empleado.dto.SolicitudEmpleadoMiniPropiaDTO;
import com.gg.turnlook.features.solicitud.empleado.dto.SolicitudEmpleadoMiniSucursalDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;






public interface SolicitudEmpleadoRepository extends JpaRepository<SolicitudEmpleado, Integer> {


    // Consulta anterior a la optimización. Reemplazada por findMiniBySucursalId.
    // List<SolicitudEmpleado> findBySucursalId(Integer sucursalId);

    // Proyeccion directa: solo los campos del DTO, sin cargar el empleado por
    // cada solicitud. El limite y el orden se resuelven en la base.
    @Query("""
                SELECT new com.gg.turnlook.features.solicitud.empleado.dto.SolicitudEmpleadoMiniSucursalDTO(
                    s.id, s.empleado.nombre, s.empleado.apellido, s.fechaSolicitud)
                FROM SolicitudEmpleado s
                WHERE s.sucursal.id = :sucursalId
                    AND s.sucursal.activo = true
                ORDER BY s.fechaSolicitud DESC, s.id DESC
            """)
    List<SolicitudEmpleadoMiniSucursalDTO> findMiniBySucursalId(
            @Param("sucursalId") Integer sucursalId,
            Pageable pageable);


    // Consulta anterior a la optimización. Reemplazada por findMiniByEmpleadoEmail.
    // List<SolicitudEmpleado> findByEmpleadoEmail(String empleadoEmail);

    @Query("""
                SELECT new com.gg.turnlook.features.solicitud.empleado.dto.SolicitudEmpleadoMiniPropiaDTO(
                    s.id, s.sucursal.nombre, s.fechaSolicitud)
                FROM SolicitudEmpleado s
                WHERE s.empleado.email = :empleadoEmail
                    AND s.sucursal.activo = true
                ORDER BY s.fechaSolicitud DESC, s.id DESC
            """)
    List<SolicitudEmpleadoMiniPropiaDTO> findMiniByEmpleadoEmail(
            @Param("empleadoEmail") String empleadoEmail,
            Pageable pageable);


    // Carga en una sola query las relaciones que usa el detalle y las
    // validaciones de aceptar/rechazar.
    @EntityGraph(attributePaths = {
            "empleado", "empleado.fotoPerfil",
            "sucursal", "sucursal.categoria", "sucursal.fotoPerfil",
            "sucursal.empleador"})
    @Query("SELECT s FROM SolicitudEmpleado s WHERE s.id = :id AND s.sucursal.activo = true")
    Optional<SolicitudEmpleado> findByIdConDetalle(@Param("id") Integer id);


    boolean existsByEmpleadoIdAndSucursalIdAndEstado(Integer empleadoId, Integer sucursalId,
                                                     EstadoSolicitud estadoSolicitud);

}
