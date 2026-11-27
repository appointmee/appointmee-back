package com.gg.turnlook.features.sucursal;

import com.gg.turnlook.features.sucursal.categoria.ECategoria;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;





public interface SucursalRepository extends JpaRepository<Sucursal, Integer> {

    @Query("""
            SELECT s.fechaCreacion, s.activo
            FROM Sucursal s
            WHERE s.fechaCreacion >= :desde AND s.fechaCreacion <= :hasta
            ORDER BY s.fechaCreacion
            """)
    List<Object[]> findAltasPorFecha(@Param("desde") LocalDate desde,
                                     @Param("hasta") LocalDate hasta);

    @Query("SELECT s.activo, COUNT(s) FROM Sucursal s GROUP BY s.activo")
    List<Object[]> contarSucursalesPorEstado();


    // categoria y fotoPerfil se traen con JOIN en la misma query. Si no, cada
    // sucursal de la lista dispara dos queries extra al acceder a la categoria
    // o a la foto (que son lazy).


    @EntityGraph(attributePaths = {"categoria", "fotoPerfil"})
    List<Sucursal> findByActivoTrue();


    boolean existsByDireccionIgnoreCase(String direccion);


    // Chequeo previo al INSERT/UPDATE. Solo compiten las sucursales activas:
    // las inactivas no tienen Place ID (se libera al dar de baja) y no deben
    // bloquear su reutilizacion. La unicidad real la garantiza el indice
    // parcial ux_sucursales_google_place_id de PostgreSQL.

    boolean existsByActivoTrueAndGooglePlaceId(String googlePlaceId);

    boolean existsByActivoTrueAndGooglePlaceIdAndIdNot(String googlePlaceId, Integer id);


    @EntityGraph(attributePaths = {"categoria", "fotoPerfil"})
    @Query("""
            SELECT s FROM Sucursal s
            WHERE s.activo = true AND s.categoria.categoria = :categoria
              AND LOCATE(REPLACE(LOWER(:nombre), ' ', ''),
                         REPLACE(LOWER(s.nombre), ' ', '')) > 0
            """)
    List<Sucursal> findByNombreContainingIgnoreCaseAndCategoriaCategoriaAndActivoTrue(
            @Param("nombre") String nombre, @Param("categoria") ECategoria categoria);


    @EntityGraph(attributePaths = {"categoria", "fotoPerfil"})
    @Query("""
            SELECT s FROM Sucursal s
            WHERE s.activo = true
              AND LOCATE(REPLACE(LOWER(:nombre), ' ', ''),
                         REPLACE(LOWER(s.nombre), ' ', '')) > 0
            """)
    List<Sucursal> findByNombreContainingIgnoreCaseAndActivoTrue(@Param("nombre") String nombre);


    @EntityGraph(attributePaths = {"categoria", "fotoPerfil"})
    List<Sucursal> findByCategoriaCategoriaAndActivoTrue(ECategoria categoria);


    // Listados globales y mapa: solo sucursales que pueden recibir solicitudes
    // de turnos. Se resuelve con EXISTS para no traer colecciones ni duplicar
    // filas por el producto de servicios y empleados. Los parametros nulos
    // desactivan el filtro de categoria o nombre; los listados internos del
    // empleador usan otras consultas y no se ven afectados.
    @EntityGraph(attributePaths = {"categoria", "fotoPerfil"})
    @Query("""
            SELECT s FROM Sucursal s
            WHERE s.activo = true
              AND (:categoria IS NULL OR s.categoria.categoria = :categoria)
              AND (:nombre IS NULL OR LOCATE(REPLACE(LOWER(:nombre), ' ', ''),
                                           REPLACE(LOWER(s.nombre), ' ', '')) > 0)
              AND EXISTS (
                  SELECT 1 FROM Servicio sv
                  WHERE sv.sucursal = s AND sv.activo = true
              )
              AND EXISTS (
                  SELECT 1 FROM s.empleados e WHERE e.activo = true
              )
            """)
    List<Sucursal> findOperativas(@Param("categoria") ECategoria categoria,
                                  @Param("nombre") String nombre);

    /** Conteo con los mismos filtros que findOperativas, para la paginacion. */
    @Query("""
            SELECT COUNT(s) FROM Sucursal s
            WHERE s.activo = true
              AND (:categoria IS NULL OR s.categoria.categoria = :categoria)
              AND (:nombre IS NULL OR LOCATE(REPLACE(LOWER(:nombre), ' ', ''),
                                           REPLACE(LOWER(s.nombre), ' ', '')) > 0
                   OR STR(s.id) = :nombre)
              AND EXISTS (
                  SELECT 1 FROM Servicio sv
                  WHERE sv.sucursal = s AND sv.activo = true
              )
              AND EXISTS (
                  SELECT 1 FROM s.empleados e WHERE e.activo = true
              )
            """)
    long contarOperativas(@Param("categoria") ECategoria categoria,
                          @Param("nombre") String nombre);

    /** Solo ids paginados, ordenados con la misma formula 50/50 del listado
     * general. La agregacion de resenias se resuelve en la base para no traer
     * todas las sucursales a memoria ni consultar resenias sucursal por
     * sucursal. El orden por id desempata y mantiene paginas estables. */
    @Query(value = """
            WITH filtradas AS (
                SELECT s.id
                FROM sucursales s
                WHERE s.activo = true
                  AND (CAST(:categoria AS VARCHAR) IS NULL
                       OR EXISTS (
                           SELECT 1 FROM categorias c
                           WHERE c.id = s.categoria_id
                             AND c.categoria = CAST(:categoria AS VARCHAR)
                       ))
                  AND (CAST(:nombre AS VARCHAR) IS NULL
                       OR POSITION(
                           REPLACE(LOWER(CAST(:nombre AS VARCHAR)), ' ', '')
                           IN REPLACE(LOWER(s.nombre), ' ', '')) > 0
                        OR CAST(s.id AS VARCHAR) = CAST(:nombre AS VARCHAR))
                  AND EXISTS (
                      SELECT 1 FROM servicios sv
                      WHERE sv.sucursal_id = s.id AND sv.activo = true
                  )
                  AND EXISTS (
                      SELECT 1 FROM sucursal_empleados se
                      JOIN usuarios u ON u.id = se.usuario_id
                      WHERE se.sucursal_id = s.id AND u.activo = true
                  )
            ),
            puntuaciones AS (
                SELECT sv.sucursal_id AS sucursal_id,
                       COUNT(r.id) AS cantidad,
                       ROUND(CAST(AVG(r.puntuacion) AS NUMERIC), 1) AS promedio
                FROM resenias r
                JOIN turnos t ON t.id = r.turno_id
                JOIN servicios sv ON sv.id = t.servicio_id
                WHERE sv.sucursal_id IN (SELECT id FROM filtradas)
                GROUP BY sv.sucursal_id
            ),
            maxima AS (
                SELECT COALESCE(MAX(cantidad), 0) AS max_cantidad FROM puntuaciones
            )
            SELECT f.id
            FROM filtradas f
            LEFT JOIN puntuaciones p ON p.sucursal_id = f.id
            CROSS JOIN maxima m
            ORDER BY
                (0.5 * CASE WHEN COALESCE(p.cantidad, 0) > 0
                            THEN (COALESCE(p.promedio, 1) - 1) / 4.0
                            ELSE 0 END
                 + 0.5 * CASE WHEN m.max_cantidad > 0
                            THEN COALESCE(p.cantidad, 0)
                                 / CAST(m.max_cantidad AS DOUBLE PRECISION)
                            ELSE 0 END) DESC,
                CASE WHEN COALESCE(p.cantidad, 0) > 0
                     THEN COALESCE(p.promedio, 1) ELSE 0 END DESC,
                COALESCE(p.cantidad, 0) DESC,
                f.id ASC
            """, nativeQuery = true)
    List<Integer> findIdsOperativasOrdenadasPorRanking(
            @Param("categoria") String categoria,
            @Param("nombre") String nombre,
            Pageable pageable);

    /** Carga en una sola query las entidades de la pagina con sus relaciones
     * usadas por SucursalMiniDTO. El orden lo aplica el servicio segun la lista
     * de ids devuelta por la consulta ordenada. */
    @EntityGraph(attributePaths = {"categoria", "fotoPerfil"})
    @Query("SELECT s FROM Sucursal s WHERE s.id IN :ids")
    List<Sucursal> findByIdsConCategoriaYFoto(@Param("ids") List<Integer> ids);

    @EntityGraph(attributePaths = {"categoria", "fotoPerfil"})
    @Query("""
            SELECT s FROM Sucursal s
            WHERE s.activo = true
              AND s.fechaCreacion >= :desde
              AND EXISTS (
                  SELECT 1 FROM Servicio sv
                  WHERE sv.sucursal = s AND sv.activo = true
              )
              AND EXISTS (
                  SELECT 1 FROM s.empleados e WHERE e.activo = true
              )
            ORDER BY s.fechaCreacion DESC, s.id DESC
            """)
    List<Sucursal> findOperativasNuevas(@Param("desde") LocalDate desde,
                                        Pageable pageable);


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


    // Sucursales donde el usuario es EMPLEADO (no empleador). Se resuelve en la
    // base con EXISTS para no cargar la coleccion completa de empleados.
    @EntityGraph(attributePaths = {"categoria", "fotoPerfil"})
    @Query("""
                SELECT s FROM Sucursal s
                WHERE s.activo = true
                AND EXISTS (
                    SELECT 1 FROM s.empleados e WHERE e.email = :email
                )
            """)
    List<Sucursal> findSucursalesDondeEsEmpleado(@Param("email") String email);


    // Variante usada por las validaciones de permisos: trae el empleador en la
    // misma query y evita el lazy load al comparar su email.
    @EntityGraph(attributePaths = {"empleador"})
    @Query("SELECT s FROM Sucursal s WHERE s.id = :id AND s.activo = true")
    Optional<Sucursal> findByIdConEmpleador(@Param("id") Integer id);


    // Detalle de sucursal: categoria, foto, empleador y empleados en una sola
    // query, para que el mapeo no dispare consultas lazy por cada relacion.
    @EntityGraph(attributePaths = {
            "categoria", "fotoPerfil", "empleador", "empleados"})
    @Query("SELECT s FROM Sucursal s WHERE s.id = :id AND s.activo = true")
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

