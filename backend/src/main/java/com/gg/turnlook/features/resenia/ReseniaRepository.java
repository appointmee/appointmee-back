package com.gg.turnlook.features.resenia;

import com.gg.turnlook.features.resenia.dto.ReseniaResponseDTO;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;







public interface ReseniaRepository extends JpaRepository<Resenia, Integer> {

    // Consulta anterior a la optimización. Reemplazada por findReseniasPorSucursal.
    // @Query("""
    //             SELECT r FROM Resenia r
    //             WHERE r.turno.servicio.sucursal.id = :sucursalId
    //             ORDER BY r.fechaResenia DESC
    //         """)
    // List<Resenia> findBySucursalId(Integer sucursalId);

    // Proyeccion directa a DTO y limite en la base. El id como desempate mantiene
    // el orden estable cuando varias resenias comparten fecha.
    @Query("""
                SELECT new com.gg.turnlook.features.resenia.dto.ReseniaResponseDTO(
                    r.puntuacion, r.comentario, r.fechaResenia)
                FROM Resenia r
                WHERE r.turno.servicio.sucursal.id = :sucursalId
                ORDER BY r.fechaResenia DESC, r.id DESC
            """)
    List<ReseniaResponseDTO> findReseniasPorSucursal(
            @Param("sucursalId") Integer sucursalId,
            Pageable pageable);


    @Query("""
                SELECT AVG(r.puntuacion)
                FROM Resenia r
                WHERE r.turno.servicio.sucursal.id = :sucursalId
            """)
    Double promedioPuntuacionesSucursal(Integer sucursalId);


    @Query("""
                SELECT AVG(r.puntuacion)
                FROM Resenia r
                WHERE r.turno.servicio.id = :servicioId
            """)
    Double promedioPuntuacionesServicio(Integer servicioId);


    @Query("""
                SELECT AVG(r.puntuacion)
                FROM Resenia r
                WHERE r.turno.empleado.id = :empleadoId
                    AND r.turno.servicio.sucursal.id = :sucursalId
            """)
    Double promedioPuntuacionesEmpleado(
            @Param("empleadoId") Integer empleadoId,
            @Param("sucursalId") Integer sucursalId);


    @Query("""
                SELECT COUNT(r)
                FROM Resenia r
                WHERE r.turno.servicio.sucursal.id = :sucursalId
            """)
    Long puntuacionesTotalesSucursal(Integer sucursalId);


    @Query("""
                SELECT COUNT(r)
                FROM Resenia r
                WHERE r.turno.servicio.id = :servicioId
            """)
    Long puntuacionesTotalesServicio(Integer servicioId);


    @Query("""
                SELECT COUNT(r)
                FROM Resenia r
                WHERE r.turno.empleado.id = :empleadoId
                    AND r.turno.servicio.sucursal.id = :sucursalId
            """)
    Long puntuacionesTotalesEmpleado(
            @Param("empleadoId") Integer empleadoId,
            @Param("sucursalId") Integer sucursalId);


    // Promedio y cantidad de resenias de varias sucursales en una sola query.
    // Devuelve una fila por sucursal: [sucursalId, promedio, cantidad].
    // El join tiene que llegar hasta la sucursal (r.turno.servicio.sucursal) y no
    // quedarse en el servicio, o el id devuelto seria el del servicio.
    // Se usa para las listas, donde consultar cada sucursal por separado
    // multiplicaba las queries por la cantidad de sucursales (N+1).

    @Query("""
                SELECT s.id, AVG(r.puntuacion), COUNT(r.id)
                FROM Resenia r
                JOIN r.turno.servicio.sucursal s
                WHERE s.id IN :sucursalIds
                GROUP BY s.id
            """)
    List<Object[]> resumenPuntuacionesPorSucursal(@Param("sucursalIds") List<Integer> sucursalIds);


    // Promedio y cantidad de resenias de varios servicios en una sola query.
    // Devuelve una fila por servicio: [servicioId, promedio, cantidad].

    @Query("""
                SELECT t.servicio.id, AVG(r.puntuacion), COUNT(r.id)
                FROM Resenia r
                JOIN r.turno t
                WHERE t.servicio.id IN :servicioIds
                GROUP BY t.servicio.id
            """)
    List<Object[]> resumenPuntuacionesPorServicio(@Param("servicioIds") List<Integer> servicioIds);


    // Promedio y cantidad de resenias de varios empleados en una sola query,
    // considerando unicamente los turnos atendidos en la sucursal indicada.
    // Devuelve una fila por empleado: [empleadoId, promedio, cantidad].

    @Query("""
                SELECT t.empleado.id, AVG(r.puntuacion), COUNT(r.id)
                FROM Resenia r
                JOIN r.turno t
                WHERE t.empleado.id IN :empleadoIds
                    AND t.servicio.sucursal.id = :sucursalId
                GROUP BY t.empleado.id
            """)
    List<Object[]> resumenPuntuacionesPorEmpleado(
            @Param("empleadoIds") List<Integer> empleadoIds,
            @Param("sucursalId") Integer sucursalId);



}



