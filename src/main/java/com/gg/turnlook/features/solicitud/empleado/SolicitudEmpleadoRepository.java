package com.gg.turnlook.features.solicitud.empleado;

import com.gg.turnlook.features.solicitud.EstadoSolicitud;
import com.gg.turnlook.features.solicitud.empleado.dto.SolicitudEmpleadoMiniPropiaDTO;
import com.gg.turnlook.features.solicitud.empleado.dto.SolicitudEmpleadoMiniSucursalDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;






public interface SolicitudEmpleadoRepository extends JpaRepository<SolicitudEmpleado, Integer> {


    // Consulta anterior a la optimización. Reemplazada por findPageMiniBySucursalId.
    // List<SolicitudEmpleado> findBySucursalId(Integer sucursalId);

    /// Listado enviado paginado: la vista previa y el historial comparten el
    /// mismo filtro y orden. El countQuery respeta las sucursales activas para
    /// que los metadatos coincidan con el contenido devuelto.
    @Query(value = """
                SELECT new com.gg.turnlook.features.solicitud.empleado.dto.SolicitudEmpleadoMiniSucursalDTO(
                    s.id, s.empleado.nombre, s.empleado.apellido, s.fechaSolicitud)
                FROM SolicitudEmpleado s
                WHERE s.sucursal.id = :sucursalId
                    AND s.sucursal.activo = true
                ORDER BY s.fechaSolicitud DESC, s.id DESC
            """,
            countQuery = """
                SELECT COUNT(s) FROM SolicitudEmpleado s
                WHERE s.sucursal.id = :sucursalId
                    AND s.sucursal.activo = true
            """)
    Page<SolicitudEmpleadoMiniSucursalDTO> findPageMiniBySucursalId(
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

    /// Listado recibido paginado: el conteo ignora las sucursales dadas de baja,
    /// igual que la consulta de contenido.
    @Query(value = """
                SELECT new com.gg.turnlook.features.solicitud.empleado.dto.SolicitudEmpleadoMiniPropiaDTO(
                    s.id, s.sucursal.nombre, s.fechaSolicitud)
                FROM SolicitudEmpleado s
                WHERE s.empleado.email = :empleadoEmail
                    AND s.sucursal.activo = true
                ORDER BY s.fechaSolicitud DESC, s.id DESC
            """,
            countQuery = """
                SELECT COUNT(s) FROM SolicitudEmpleado s
                WHERE s.empleado.email = :empleadoEmail
                    AND s.sucursal.activo = true
            """)
    Page<SolicitudEmpleadoMiniPropiaDTO> findPageByEmpleadoEmail(
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
