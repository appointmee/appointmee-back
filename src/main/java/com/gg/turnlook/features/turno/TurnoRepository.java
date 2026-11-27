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

    @Query("""
            SELECT t.fechaHora, t.estado
            FROM Turno t
            WHERE t.fechaHora >= :desde AND t.fechaHora < :hasta
            ORDER BY t.fechaHora
            """)
    List<Object[]> findDatosEstadisticasGlobales(@Param("desde") LocalDateTime desde,
                                                @Param("hasta") LocalDateTime hasta);

    @Query("""
            SELECT t.fechaHora, t.estado, t.servicio.id, t.servicio.nombre
            FROM Turno t
            WHERE t.servicio.sucursal.id = :sucursalId
              AND t.fechaHora >= :desde AND t.fechaHora < :hasta
            ORDER BY t.fechaHora
            """)
    List<Object[]> findDatosEstadisticas(@Param("sucursalId") Integer sucursalId,
                                         @Param("desde") LocalDateTime desde,
                                         @Param("hasta") LocalDateTime hasta);

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

    /**
     * Turnos cancelables de un empleado. Las relaciones se cargan junto con el
     * turno para poder avisar a las partes sin acceder a lazy loading fuera de
     * la transacción que realiza la baja.
     */
    @EntityGraph(attributePaths = {
            "cliente", "empleado", "empleado.fotoPerfil",
            "servicio", "servicio.sucursal", "servicio.sucursal.empleador",
            "resenia"})
    @Query("SELECT t FROM Turno t WHERE t.empleado.id = :empleadoId AND t.estado IN :estados")
    List<Turno> findCancelablesPorEmpleado(
            @Param("empleadoId") Integer empleadoId,
            @Param("estados") Collection<EstadoTurno> estados);

    /** Turnos cancelables de una sucursal, con las mismas relaciones cargadas. */
    @EntityGraph(attributePaths = {
            "cliente", "empleado", "empleado.fotoPerfil",
            "servicio", "servicio.sucursal", "servicio.sucursal.empleador",
            "resenia"})
    @Query("SELECT t FROM Turno t WHERE t.servicio.sucursal.id = :sucursalId AND t.estado IN :estados")
    List<Turno> findCancelablesPorSucursal(
            @Param("sucursalId") Integer sucursalId,
            @Param("estados") Collection<EstadoTurno> estados);

    /** Turnos cancelables de un empleado en una sucursal concreta. */
    @EntityGraph(attributePaths = {
            "cliente", "empleado", "empleado.fotoPerfil",
            "servicio", "servicio.sucursal", "servicio.sucursal.empleador",
            "resenia"})
    @Query("""
            SELECT t FROM Turno t
            WHERE t.empleado.id = :empleadoId
                AND t.servicio.sucursal.id = :sucursalId
                AND t.estado IN :estados
            """)
    List<Turno> findCancelablesPorEmpleadoYSucursal(
            @Param("empleadoId") Integer empleadoId,
            @Param("sucursalId") Integer sucursalId,
            @Param("estados") Collection<EstadoTurno> estados);

    /** Demanda reciente agrupada en base: un solo GROUP BY para todas las
     * sucursales, sin consultas por sucursal. Solo turnos confirmados/realizados. */
    @Query("""
            SELECT sv.sucursal.id, COUNT(t)
            FROM Turno t JOIN t.servicio sv
            WHERE t.estado IN :estados
              AND t.fechaHora >= :desde
              AND sv.sucursal.activo = true
              AND EXISTS (
                  SELECT 1 FROM Servicio s2
                  WHERE s2.sucursal = sv.sucursal AND s2.activo = true
              )
              AND EXISTS (
                  SELECT 1 FROM sv.sucursal.empleados e WHERE e.activo = true
              )
            GROUP BY sv.sucursal.id
            ORDER BY COUNT(t) DESC, sv.sucursal.id ASC
            """)
    List<Object[]> contarTurnosRecientesPorSucursal(
            @Param("estados") Collection<EstadoTurno> estados,
            @Param("desde") LocalDateTime desde,
            Pageable pageable);

    /** Frecuencia ponderada por categoria de los turnos del cliente en los
     * ultimos seis meses. Los pesos 1 / 0.7 / 0.4 se aplican en la consulta. */
    @Query("""
            SELECT t.servicio.sucursal.categoria.categoria,
                   SUM(CASE
                       WHEN t.fechaHora >= :hace30 THEN 1.0
                       WHEN t.fechaHora >= :hace90 THEN 0.7
                       ELSE 0.4 END)
            FROM Turno t
            WHERE t.cliente.email = :email
              AND t.estado IN :estados
              AND t.fechaHora >= :hace180
            GROUP BY t.servicio.sucursal.categoria.categoria
            ORDER BY SUM(CASE
                       WHEN t.fechaHora >= :hace30 THEN 1.0
                       WHEN t.fechaHora >= :hace90 THEN 0.7
                       ELSE 0.4 END) DESC,
                     t.servicio.sucursal.categoria.categoria ASC
            """)
    List<Object[]> puntuarCategoriasPorCliente(
            @Param("email") String email,
            @Param("estados") Collection<EstadoTurno> estados,
            @Param("hace30") LocalDateTime hace30,
            @Param("hace90") LocalDateTime hace90,
            @Param("hace180") LocalDateTime hace180);

    /** Sucursales que el cliente ya reservo con turnos vigentes o realizados. */
    @Query("""
            SELECT DISTINCT t.servicio.sucursal.id
            FROM Turno t
            WHERE t.cliente.email = :email
              AND t.estado IN :estados
            """)
    List<Integer> findSucursalesYaReservadasPorCliente(
            @Param("email") String email,
            @Param("estados") Collection<EstadoTurno> estados);


}
