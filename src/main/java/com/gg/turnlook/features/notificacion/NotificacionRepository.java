package com.gg.turnlook.features.notificacion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
     * Listado paginado del usuario. {@code leida == null} significa "sin filtro".
     * El orden se fija aquí para que la paginación sea estable.
     */
    @Query("""
            select n
            from Notificacion n
            where n.usuario.email = :email
              and (:leida is null or n.leida = :leida)
            order by n.fechaCreacion desc, n.id desc
            """)
    Page<Notificacion> listarPorUsuario(@Param("email") String email,
                                        @Param("leida") Boolean leida,
                                        Pageable pageable);

    /** Contador de no leídas calculado en la base de datos. */
    @Query("""
            select count(n)
            from Notificacion n
            where n.usuario.email = :email and n.leida = false
            """)
    long contarNoLeidas(@Param("email") String email);
}
