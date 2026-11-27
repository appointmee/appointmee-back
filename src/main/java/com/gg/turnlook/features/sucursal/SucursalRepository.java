package com.gg.turnlook.features.sucursal;

import com.gg.turnlook.features.sucursal.categoria.ECategoria;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
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
    // filas por el producto de servicios y empleados.
    //
    // Los filtros opcionales se normalizan en Java y llegan siempre como valores
    // no nulos. Hibernate no puede inferir el tipo de un parametro null usado
    // como argumento de LOWER(): en PostgreSQL lo enlaza como bytea y falla con
    // "function lower(bytea) does not exist". Normalizando aca la consulta ya no
    // contiene LOWER(?) y el parametro siempre se enlaza como VARCHAR.
    default List<Sucursal> findOperativas(ECategoria categoria, String nombre) {
        String filtro = nombre == null || nombre.isBlank() ? null : nombre.trim();
        return findOperativasFiltradas(
                categoria, filtro != null, normalizarBusqueda(filtro));
    }

    @EntityGraph(attributePaths = {"categoria", "fotoPerfil"})
    @Query("""
            SELECT s FROM Sucursal s
            WHERE s.activo = true
              AND (:categoria IS NULL OR s.categoria.categoria = :categoria)
              AND (:filtrarNombre = false
                   OR LOCATE(:nombreNormalizado,
                             REPLACE(LOWER(s.nombre), ' ', '')) > 0)
              AND EXISTS (
                  SELECT 1 FROM Servicio sv
                  WHERE sv.sucursal = s AND sv.activo = true
              )
              AND EXISTS (
                  SELECT 1 FROM s.empleados e WHERE e.activo = true
              )
            """)
    List<Sucursal> findOperativasFiltradas(
            @Param("categoria") ECategoria categoria,
            @Param("filtrarNombre") boolean filtrarNombre,
            @Param("nombreNormalizado") String nombreNormalizado);

    /** Conteo con los mismos filtros que findOperativas, para la paginacion. */
    default long contarOperativas(ECategoria categoria, String nombre) {
        String filtro = nombre == null || nombre.isBlank() ? null : nombre.trim();
        return contarOperativasFiltradas(
                categoria, filtro != null, normalizarBusqueda(filtro), filtro);
    }

    @Query("""
            SELECT COUNT(s) FROM Sucursal s
            WHERE s.activo = true
              AND (:categoria IS NULL OR s.categoria.categoria = :categoria)
              AND (:filtrarNombre = false
                   OR LOCATE(:nombreNormalizado,
                             REPLACE(LOWER(s.nombre), ' ', '')) > 0
                   OR STR(s.id) = :nombreOriginal)
              AND EXISTS (
                  SELECT 1 FROM Servicio sv
                  WHERE sv.sucursal = s AND sv.activo = true
              )
              AND EXISTS (
                  SELECT 1 FROM s.empleados e WHERE e.activo = true
              )
            """)
    long contarOperativasFiltradas(
            @Param("categoria") ECategoria categoria,
            @Param("filtrarNombre") boolean filtrarNombre,
            @Param("nombreNormalizado") String nombreNormalizado,
            @Param("nombreOriginal") String nombreOriginal);

    private static String normalizarBusqueda(String nombre) {
        return nombre == null ? "" : nombre.toLowerCase(Locale.ROOT).replace(" ", "");
    }

    /** Solo ids paginados, ordenados con la misma formula 50/50 del listado
     * general. La agregacion de resenias se resuelve en la base para no traer
     * todas las sucursales a memoria ni consultar resenias sucursal por
     * sucursal. El orden por id desempata y mantiene paginas estables.
     *
     * Igual que en findOperativas, los filtros opcionales se normalizan en Java
     * y llegan como valores no nulos junto con sus flags: asi Hibernate no tiene
     * que inferir el tipo de un null usado dentro de LOWER()/POSITION(). */
    default List<Integer> findIdsOperativasOrdenadasPorRanking(
            String categoria, String nombre, Pageable pageable) {
        String nombreFiltro = nombre == null || nombre.isBlank() ? null : nombre.trim();
        return findIdsOperativasOrdenadasPorRankingFiltrado(
                categoria, categoria != null,
                nombreFiltro != null, normalizarBusqueda(nombreFiltro),
                nombreFiltro, pageable);
    }

    @Query(value = """
            WITH filtradas AS (
                SELECT s.id
                FROM sucursales s
                WHERE s.activo = true
                  AND (:filtrarCategoria = false
                       OR EXISTS (
                           SELECT 1 FROM categorias c
                           WHERE c.id = s.categoria_id
                             AND c.categoria = :categoria
                       ))
                  AND (:filtrarNombre = false
                       OR POSITION(:nombreNormalizado
                                   IN REPLACE(LOWER(s.nombre), ' ', '')) > 0
                       OR CAST(s.id AS VARCHAR) = :nombreOriginal)
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
    List<Integer> findIdsOperativasOrdenadasPorRankingFiltrado(
            @Param("categoria") String categoria,
            @Param("filtrarCategoria") boolean filtrarCategoria,
            @Param("filtrarNombre") boolean filtrarNombre,
            @Param("nombreNormalizado") String nombreNormalizado,
            @Param("nombreOriginal") String nombreOriginal,
            Pageable pageable);

    /** Busqueda libre del listado administrativo: nombre, categoria o id.
     * Mantiene el mismo ranking 50/50 y resuelve filtro y limite en la base. */
    default List<Integer> findIdsOperativasParaAdmin(String termino, Pageable pageable) {
        String filtro = termino == null || termino.isBlank() ? null : termino.trim();
        return findIdsOperativasAdminFiltrado(
                filtro != null, normalizarBusqueda(filtro), filtro, pageable);
    }

    @Query(value = """
            WITH filtradas AS (
                SELECT s.id
                FROM sucursales s
                WHERE s.activo = true
                  AND (:filtrarTermino = false
                       OR POSITION(:terminoNormalizado
                                   IN REPLACE(LOWER(s.nombre), ' ', '')) > 0
                       OR CAST(s.id AS VARCHAR) = :terminoOriginal
                       OR EXISTS (
                           SELECT 1 FROM categorias c
                           WHERE c.id = s.categoria_id
                             AND POSITION(:terminoNormalizado
                                          IN LOWER(CAST(c.categoria AS VARCHAR))) > 0
                       ))
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
    List<Integer> findIdsOperativasAdminFiltrado(
            @Param("filtrarTermino") boolean filtrarTermino,
            @Param("terminoNormalizado") String terminoNormalizado,
            @Param("terminoOriginal") String terminoOriginal,
            Pageable pageable);

    /** Total con el mismo filtro libre que la consulta de contenido. */
    default long contarOperativasParaAdmin(String termino) {
        String filtro = termino == null || termino.isBlank() ? null : termino.trim();
        return contarOperativasAdminFiltrado(
                filtro != null, normalizarBusqueda(filtro), filtro);
    }

    @Query("""
            SELECT COUNT(s) FROM Sucursal s
            WHERE s.activo = true
              AND (:filtrarTermino = false
                   OR LOCATE(:terminoNormalizado,
                             REPLACE(LOWER(s.nombre), ' ', '')) > 0
                   OR STR(s.id) = :terminoOriginal
                   OR LOCATE(:terminoNormalizado,
                             LOWER(CAST(s.categoria.categoria AS string))) > 0)
              AND EXISTS (
                  SELECT 1 FROM Servicio sv
                  WHERE sv.sucursal = s AND sv.activo = true
              )
              AND EXISTS (
                  SELECT 1 FROM s.empleados e WHERE e.activo = true
              )
            """)
    long contarOperativasAdminFiltrado(
            @Param("filtrarTermino") boolean filtrarTermino,
            @Param("terminoNormalizado") String terminoNormalizado,
            @Param("terminoOriginal") String terminoOriginal);

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

    /**
     * Pertenencia del empleado asignado a un turno. Consulta la relación actual
     * (sucursal_empleados) sin filtrar por activo: la baja lógica conserva la
     * fila y el detalle del turno debe poder distinguir ese escenario del de
     * una desvinculación.
     */
    @Query("""
                SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
                FROM Sucursal s JOIN s.empleados e
                WHERE s.id = :sucursalId AND e.id = :empleadoId
            """)
    boolean existeEmpleadoEnSucursal(
            @Param("sucursalId") Integer sucursalId,
            @Param("empleadoId") Integer empleadoId);


}

