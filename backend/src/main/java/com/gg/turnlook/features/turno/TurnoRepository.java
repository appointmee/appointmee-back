package com.gg.turnlook.features.turno;

import com.gg.turnlook.features.turno.dto.TurnoDisponibilidadDTO;
import com.gg.turnlook.features.turno.dto.TurnoMiniDTO;
import com.gg.turnlook.features.usuario.Usuario;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;




public interface TurnoRepository extends JpaRepository<Turno, Integer> {

    @Modifying
    @Query("update Turno t set t.estado = :nuevoEstado, t.fechaModificacionEstado = CURRENT_TIMESTAMP where t.id = :turnoId and t.estado in :estadosActuales")
    int actualizarEstadoSiEs(@Param("turnoId") Integer turnoId,
                             @Param("estadosActuales") Collection<EstadoTurno> estadosActuales,
                             @Param("nuevoEstado") EstadoTurno nuevoEstado);


    // Recupera de forma eficiente los turnos PENDIENTE cuya ultima modificacion
    // de estado (o fecha de creacion si no tiene) es anterior o igual al limite.
    @Query("SELECT t FROM Turno t WHERE t.estado = :estado AND COALESCE(t.fechaModificacionEstado, t.fechaReserva) <= :fechaLimite")
    List<Turno> findPendientesVencidos(@Param("estado") EstadoTurno estado,
                                       @Param("fechaLimite") LocalDateTime fechaLimite);


    // Cancelacion masiva condicional: solo afecta turnos que sigan PENDIENTE y
    // vencidos al momento de ejecutar la actualizacion.
    @Modifying
    @Query("update Turno t set t.estado = :nuevoEstado, t.fechaModificacionEstado = CURRENT_TIMESTAMP where t.estado = :estadoActual and COALESCE(t.fechaModificacionEstado, t.fechaReserva) <= :fechaLimite")
    int cancelarTurnosPendientesVencidos(@Param("estadoActual") EstadoTurno estadoActual,
                                          @Param("nuevoEstado") EstadoTurno nuevoEstado,
                                          @Param("fechaLimite") LocalDateTime fechaLimite);


    // Consulta anterior a la optimización. Reemplazada por findDisponibilidadPorEmpleado.
    // List<Turno> findByEmpleadoAndFechaHoraBetweenAndEstadoNot(Usuario empleado,
    //                                               LocalDateTime inicioDia, LocalDateTime finDia,
    //                                                           EstadoTurno estadoTurno);

    // Trae solamente la fecha y la duracion del servicio. Antes se cargaba el turno
    // completo y, por cada uno, se accedia de forma lazy a su servicio.
    @Query("""
                SELECT new com.gg.turnlook.features.turno.dto.TurnoDisponibilidadDTO(
                    t.fechaHora, t.servicio.duracion)
                FROM Turno t
                WHERE t.empleado = :empleado
                    AND t.fechaHora BETWEEN :inicioDia AND :finDia
                    AND t.estado NOT IN :estadosExcluidos
            """)
    List<TurnoDisponibilidadDTO> findDisponibilidadPorEmpleado(
            @Param("empleado") Usuario empleado,
            @Param("inicioDia") LocalDateTime inicioDia,
            @Param("finDia") LocalDateTime finDia,
            @Param("estadosExcluidos") Collection<EstadoTurno> estadosExcluidos);


    // Consulta anterior a la optimización. Reemplazada por findMiniByServicioSucursalIdAndEstado.
    // List<Turno> findByServicioSucursalIdAndEstado(Integer sucursalId, EstadoTurno estadoTurno);

    @Query("""
                SELECT new com.gg.turnlook.features.turno.dto.TurnoMiniDTO(
                    t.id, t.servicio.nombre, t.fechaHora, t.estado, r.puntuacion)
                FROM Turno t
                LEFT JOIN t.resenia r
                WHERE t.servicio.sucursal.id = :sucursalId
                    AND t.estado = :estadoTurno
                ORDER BY t.fechaHora DESC, t.id DESC
            """)
    List<TurnoMiniDTO> findMiniByServicioSucursalIdAndEstado(
            @Param("sucursalId") Integer sucursalId,
            @Param("estadoTurno") EstadoTurno estadoTurno,
            Pageable pageable);


    // Consulta anterior a la optimización. Reemplazada por findMiniByClienteEmail.
    // List<Turno> findByClienteEmail(String clienteEmail);

    @Query("""
                SELECT new com.gg.turnlook.features.turno.dto.TurnoMiniDTO(
                    t.id, t.servicio.nombre, t.fechaHora, t.estado, r.puntuacion)
                FROM Turno t
                LEFT JOIN t.resenia r
                WHERE t.cliente.email = :clienteEmail
                ORDER BY t.fechaHora DESC, t.id DESC
            """)
    List<TurnoMiniDTO> findMiniByClienteEmail(
            @Param("clienteEmail") String clienteEmail,
            Pageable pageable);


    // Consulta anterior a la optimización. Reemplazada por
    // findMiniByEmpleadoEmailAndServicioSucursalIdAndEstado.
    // List<Turno> findByEmpleadoEmailAndServicioSucursalIdAndEstado(
    //         String empleadoEmail, Integer sucursalId, EstadoTurno estadoTurno);

    @Query("""
                SELECT new com.gg.turnlook.features.turno.dto.TurnoMiniDTO(
                    t.id, t.servicio.nombre, t.fechaHora, t.estado, r.puntuacion)
                FROM Turno t
                LEFT JOIN t.resenia r
                WHERE t.empleado.email = :empleadoEmail
                    AND t.servicio.sucursal.id = :sucursalId
                    AND t.estado = :estadoTurno
                ORDER BY t.fechaHora DESC, t.id DESC
            """)
    List<TurnoMiniDTO> findMiniByEmpleadoEmailAndServicioSucursalIdAndEstado(
            @Param("empleadoEmail") String empleadoEmail,
            @Param("sucursalId") Integer sucursalId,
            @Param("estadoTurno") EstadoTurno estadoTurno,
            Pageable pageable);


    // Carga en una sola query las relaciones que usan los detalles y las
    // validaciones de cancelacion/finalizacion/resenia.
    @EntityGraph(attributePaths = {
            "cliente", "empleado", "empleado.fotoPerfil",
            "servicio", "servicio.sucursal", "servicio.sucursal.empleador",
            "resenia"})
    @Query("SELECT t FROM Turno t WHERE t.id = :id")
    Optional<Turno> findByIdConDetalles(@Param("id") Integer id);


    // Los webhooks solo necesitan el estado; evita cargar la entidad completa.
    @Query("SELECT t.estado FROM Turno t WHERE t.id = :id")
    Optional<EstadoTurno> findEstadoById(@Param("id") Integer id);


}
