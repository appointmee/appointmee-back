package com.gg.turnlook.Backend.Repository;



import com.gg.turnlook.Backend.Model.Resenia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;



public interface ReseniaRepository extends JpaRepository<Resenia, Integer> {

    @Query("""
                SELECT r FROM Resenia r
                WHERE r.turno.servicio.sucursal.id = :sucursalId
                ORDER BY r.fechaResenia DESC
            """)
    List<Resenia> findBySucursalId(Integer sucursalId);


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
            """)
    Double promedioPuntuacionesEmpleado(Integer empleadoId);


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
            """)
    Long puntuacionesTotalesEmpleado(Integer empleadoId);



}



