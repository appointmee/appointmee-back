package com.gg.turnlook.features.solicitud.empleador;

import com.gg.turnlook.features.solicitud.EstadoSolicitud;
import com.gg.turnlook.features.solicitud.empleador.dto.SolicitudEmpleadorMiniDTO;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;





public interface SolicitudEmpleadorRepository extends JpaRepository<SolicitudEmpleador, Integer> {


    boolean existsByUsuarioIdAndEstado(Integer userId, EstadoSolicitud estado);


    // Consulta anterior a la optimización. Reemplazada por findMiniByEstado.
    // List<SolicitudEmpleador> findByEstado(EstadoSolicitud estado);

    // Proyeccion directa: solo los campos del DTO, sin cargar el usuario por
    // cada solicitud. El limite y el orden se resuelven en la base.
    @Query("""
                SELECT new com.gg.turnlook.features.solicitud.empleador.dto.SolicitudEmpleadorMiniDTO(
                    s.id, s.usuario.nombre, s.usuario.apellido, s.fechaSolicitud)
                FROM SolicitudEmpleador s
                WHERE s.estado = :estado
                ORDER BY s.fechaSolicitud DESC, s.id DESC
            """)
    List<SolicitudEmpleadorMiniDTO> findMiniByEstado(
            @Param("estado") EstadoSolicitud estado,
            Pageable pageable);


    // Consulta anterior a la optimización. Reemplazada por findMiniByUsuarioEmail.
    // List<SolicitudEmpleador> findByUsuarioEmail(String userEmail);

    @Query("""
                SELECT new com.gg.turnlook.features.solicitud.empleador.dto.SolicitudEmpleadorMiniDTO(
                    s.id, s.usuario.nombre, s.usuario.apellido, s.fechaSolicitud)
                FROM SolicitudEmpleador s
                WHERE s.usuario.email = :userEmail
                ORDER BY s.fechaSolicitud DESC, s.id DESC
            """)
    List<SolicitudEmpleadorMiniDTO> findMiniByUsuarioEmail(
            @Param("userEmail") String userEmail,
            Pageable pageable);


}



