package com.gg.turnlook.features.sucursal;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SucursalVisitaRepository extends JpaRepository<SucursalVisita, Integer> {

    Optional<SucursalVisita> findByUsuarioIdAndSucursalId(Integer usuarioId, Integer sucursalId);

    /** Devuelve solo sucursales operativas, sin repetirlas, ordenadas por ultima visita. */
    @EntityGraph(attributePaths = {"sucursal", "sucursal.categoria", "sucursal.fotoPerfil"})
    @Query("""
            SELECT v.sucursal FROM SucursalVisita v
            WHERE v.usuario.id = :usuarioId
              AND v.sucursal.activo = true
              AND EXISTS (
                  SELECT 1 FROM Servicio sv
                  WHERE sv.sucursal = v.sucursal AND sv.activo = true
              )
              AND EXISTS (
                  SELECT 1 FROM v.sucursal.empleados e WHERE e.activo = true
              )
            ORDER BY v.fechaVisita DESC, v.sucursal.id ASC
            """)
    List<Sucursal> findSucursalesRecientesOperativas(
            @Param("usuarioId") Integer usuarioId, Pageable pageable);

}
