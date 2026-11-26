package com.gg.turnlook.features.solicitud.empleador;

import com.gg.turnlook.features.solicitud.EstadoSolicitud;
import com.gg.turnlook.features.solicitud.empleador.dto.SolicitudEmpleadorMiniDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;




public interface SolicitudEmpleadorRepository extends JpaRepository<SolicitudEmpleador, Integer> {


    boolean existsByUsuarioIdAndEstado(Integer userId, EstadoSolicitud estado);


    /// Listado administrativo: todos los estados cuando :estado es NULL y solo
    /// el estado pedido cuando llega un filtro. El filtro se aplica en SQL, no
    /// en memoria, y la proyeccion evita cargar entidades ni colecciones.
    @Query("""
                SELECT new com.gg.turnlook.features.solicitud.empleador.dto.SolicitudEmpleadorMiniDTO(
                    s.id, s.usuario.nombre, s.usuario.apellido, s.fechaSolicitud, s.estado)
                FROM SolicitudEmpleador s
                WHERE :estado IS NULL OR s.estado = :estado
                ORDER BY s.fechaSolicitud DESC, s.id DESC
            """)
    List<SolicitudEmpleadorMiniDTO> findMiniAdmin(
            @Param("estado") EstadoSolicitud estado,
            Pageable pageable);


    @Query("""
                SELECT new com.gg.turnlook.features.solicitud.empleador.dto.SolicitudEmpleadorMiniDTO(
                    s.id, s.usuario.nombre, s.usuario.apellido, s.fechaSolicitud, s.estado)
                FROM SolicitudEmpleador s
                WHERE s.usuario.email = :userEmail
                ORDER BY s.fechaSolicitud DESC, s.id DESC
            """)
    List<SolicitudEmpleadorMiniDTO> findMiniByUsuarioEmail(
            @Param("userEmail") String userEmail,
            Pageable pageable);


    /// Detalle en una sola consulta: usuario y ubicaciones se cargan con JOIN,
    /// sin una consulta por ubicacion (evita N+1).
    @Query("""
                SELECT DISTINCT s FROM SolicitudEmpleador s
                LEFT JOIN FETCH s.usuario
                LEFT JOIN FETCH s.ubicaciones
                WHERE s.id = :solicitudId
            """)
    Optional<SolicitudEmpleador> findByIdConUbicaciones(
            @Param("solicitudId") Integer solicitudId);


}
