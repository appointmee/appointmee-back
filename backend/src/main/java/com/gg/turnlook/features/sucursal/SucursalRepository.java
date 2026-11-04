package com.gg.turnlook.features.sucursal;

import com.gg.turnlook.features.sucursal.categoria.ECategoria;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;





public interface SucursalRepository extends JpaRepository<Sucursal, Integer> {


    // categoria y fotoPerfil se traen con JOIN en la misma query. Si no, cada
    // sucursal de la lista dispara dos queries extra al acceder a la categoria
    // o a la foto (que son lazy).


    @EntityGraph(attributePaths = {"categoria", "fotoPerfil"})
    List<Sucursal> findByActivoTrue();


    boolean existsByDireccionIgnoreCase(String direccion);


    @EntityGraph(attributePaths = {"categoria", "fotoPerfil"})
    List<Sucursal> findByNombreContainingIgnoreCaseAndCategoriaCategoriaAndActivoTrue(
            String nombre, ECategoria categoria);


    @EntityGraph(attributePaths = {"categoria", "fotoPerfil"})
    List<Sucursal> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre);


    @EntityGraph(attributePaths = {"categoria", "fotoPerfil"})
    List<Sucursal> findByCategoriaCategoriaAndActivoTrue(ECategoria categoria);


    @EntityGraph(attributePaths = {"categoria", "fotoPerfil"})
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


    // Variante usada por las validaciones de permisos: trae el empleador en la
    // misma query y evita el lazy load al comparar su email.
    @EntityGraph(attributePaths = {"empleador"})
    @Query("SELECT s FROM Sucursal s WHERE s.id = :id")
    Optional<Sucursal> findByIdConEmpleador(@Param("id") Integer id);


    // Detalle de sucursal: categoria, foto, empleador y empleados en una sola
    // query, para que el mapeo no dispare consultas lazy por cada relacion.
    @EntityGraph(attributePaths = {
            "categoria", "fotoPerfil", "empleador", "empleados"})
    @Query("SELECT s FROM Sucursal s WHERE s.id = :id")
    Optional<Sucursal> findByIdConDetalle(@Param("id") Integer id);


    // Verificacion de pertenencia resuelta en la base, sin cargar la coleccion
    // completa de empleados de la sucursal.
    @Query("""
                SELECT COUNT(s) FROM Sucursal s
                WHERE s.id = :sucursalId
                    AND (
                        s.empleador.email = :email
                        OR EXISTS (
                            SELECT 1 FROM s.empleados e WHERE e.email = :email
                        )
                    )
            """)
    long contarUsuarioEnSucursal(
            @Param("sucursalId") Integer sucursalId,
            @Param("email") String email);


}


