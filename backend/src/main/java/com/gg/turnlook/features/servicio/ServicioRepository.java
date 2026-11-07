package com.gg.turnlook.features.servicio;

import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;



public interface ServicioRepository extends JpaRepository<Servicio, Integer> {

    List<Servicio> findByActivoTrue();

    // fotoPerfil viene con JOIN para no consultarla una vez por servicio.
    @EntityGraph(attributePaths = {"fotoPerfil"})
    List<Servicio> findBySucursalId(Integer sucursalId);


    // Detalle de servicio: foto y sucursal con su categoria en una sola query,
    // que es todo lo que necesita ServicioResponseDTO.
    @EntityGraph(attributePaths = {"fotoPerfil", "sucursal", "sucursal.categoria"})
    @Query("SELECT s FROM Servicio s WHERE s.id = :id")
    Optional<Servicio> findByIdConDetalle(@Param("id") Integer id);
}
