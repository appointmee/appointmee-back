package com.gg.turnlook.Backend.Repository;


import com.gg.turnlook.Backend.Enum.ECategoria;
import com.gg.turnlook.Backend.Model.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;


public interface SucursalRepository extends JpaRepository<Sucursal, Integer> {


    List<Sucursal> findByActivoTrue();


    boolean existsByDireccionIgnoreCase(String direccion);


    List<Sucursal> findByNombreContainingIgnoreCaseAndCategoriaCategoriaAndActivoTrue(
            String nombre, ECategoria categoria);


    List<Sucursal> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre);


    List<Sucursal> findByCategoriaCategoriaAndActivoTrue(ECategoria categoria);


    @Query("""
                SELECT s FROM Sucursal s
                WHERE s.activo = true
                AND (
                    s.empleador.email = :email
                    OR :email IN (
                        SELECT e.email FROM s.empleados e
                    )
                )
            """)
    List<Sucursal> findSucursalesDondeEsta(String email);


}


