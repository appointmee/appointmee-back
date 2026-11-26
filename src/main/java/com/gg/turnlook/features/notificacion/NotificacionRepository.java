package com.gg.turnlook.features.notificacion;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Consultas de notificaciones siempre acotadas al usuario.
 *
 * <p>No existe un método de listado global: el email del autenticado es parte
 * obligatoria de cada consulta, de modo que una cuenta no pueda acceder a las
 * notificaciones de otra.</p>
 */
public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {

    /**
     * Listado paginado del usuario sin filtro de tipo.
     * Se conserva para los llamadores que no conocen el filtro nuevo.
     */
    default Page<Notificacion> listarPorUsuario(String email, Boolean leida, Pageable pageable) {
        return listarPorUsuario(email, null, leida, pageable);
    }

    /**
     * Listado paginado del usuario. {@code tipo} y {@code leida} nulos significan
     * "sin filtro"; combinar ambos no descarta resultados de la otra dimensión.
     * El orden se fija aquí para que la paginación sea estable.
     */
    @Query("""
            select n
            from Notificacion n
            where n.usuario.email = :email
              and (:tipo is null or n.tipo = :tipo)
              and (:leida is null or n.leida = :leida)
            order by n.fechaCreacion desc, n.id desc
            """)
    Page<Notificacion> listarPorUsuario(@Param("email") String email,
                                        @Param("tipo") TipoNotificacion tipo,
                                        @Param("leida") Boolean leida,
                                        Pageable pageable);

    /** Una notificación sólo es visible para su dueño. */
    Optional<Notificacion> findByIdAndUsuarioEmail(Integer id, String email);

    /** Contador de no leídas calculado en la base de datos. */
    @Query("""
            select count(n)
            from Notificacion n
            where n.usuario.email = :email and n.leida = false
            """)
    long contarNoLeidas(@Param("email") String email);

    /**
     * Marca como leídas todas las no leídas del usuario, opcionalmente de un tipo.
     * La restricción por email evita alterar notificaciones de otras cuentas.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Notificacion n set n.leida = true
            where n.usuario.email = :email
              and n.leida = false
              and (:tipo is null or n.tipo = :tipo)
            """)
    int marcarTodasLeidas(@Param("email") String email, @Param("tipo") TipoNotificacion tipo);
}
