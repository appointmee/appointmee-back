package com.gg.turnlook.features.usuario;

import com.gg.turnlook.features.usuario.dto.UsuarioMiniAdminDTO;
import com.gg.turnlook.features.usuario.rol.ERol;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;




public interface UsuarioRepository extends JpaRepository<Usuario,Integer> {

    @Query("""
            SELECT u.fechaCreacion, r.rol
            FROM Usuario u JOIN u.roles r
            WHERE u.fechaCreacion >= :desde AND u.fechaCreacion <= :hasta
              AND r.rol IN :roles
            ORDER BY u.fechaCreacion
            """)
    List<Object[]> findAltasPorFechaYRol(@Param("desde") LocalDate desde,
                                         @Param("hasta") LocalDate hasta,
                                         @Param("roles") List<ERol> roles);

    @Query("""
            SELECT r.rol, COUNT(DISTINCT u.id)
            FROM Usuario u JOIN u.roles r
            WHERE u.activo = true AND r.rol IN :roles
            GROUP BY r.rol
            """)
    List<Object[]> contarUsuariosActivosPorRol(@Param("roles") List<ERol> roles);


    // fotoPerfil se trae junto con los roles porque el perfil y varias
    // respuestas de usuario necesitan la foto sin disparar una query extra.
    @EntityGraph(attributePaths = {"roles", "fotoPerfil"})
    Optional<Usuario> findByEmailAndActivoTrue(String email);

    @EntityGraph(attributePaths = {"roles"})
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> findByIdConRoles(@Param("id") Integer id);

    // Perfil completo: roles, foto y sucursales con su categoria y foto.
    @EntityGraph(attributePaths = {
            "roles", "fotoPerfil",
            "sucursalesEmpleado", "sucursalesEmpleado.categoria",
            "sucursalesEmpleado.fotoPerfil"})
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> findByIdConPerfil(@Param("id") Integer id);

    @Query("select u.id from Usuario u where u.email = :email and u.activo = true")
    Optional<Integer> findIdByEmailAndActivoTrue(@Param("email") String email);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> bloquearPorId(Integer id);

    boolean existsByEmail(String email);

    Optional<Usuario> findByEmailAndActivoFalse(String email);

    boolean existsByEmailAndActivoFalse(String email);


    // Listado administrativo: los filtros se resuelven en la base y solo se
    // proyectan los campos que devuelve UsuarioMiniAdminDTO.
    @Query("""
                SELECT new com.gg.turnlook.features.usuario.dto.UsuarioMiniAdminDTO(
                    u.id, u.nombre, u.apellido, u.activo, foto.url)
                FROM Usuario u
                LEFT JOIN u.fotoPerfil foto
                WHERE (CAST(:nombre AS string) IS NULL
                        OR LOWER(u.nombre) = LOWER(CAST(:nombre AS string)))
                    AND (CAST(:apellido AS string) IS NULL
                        OR LOWER(u.apellido) = LOWER(CAST(:apellido AS string)))
                    AND (CAST(:activo AS boolean) IS NULL OR u.activo = :activo)
                ORDER BY u.id
            """)
    List<UsuarioMiniAdminDTO> buscarMiniAdmin(
            @Param("nombre") String nombre,
            @Param("apellido") String apellido,
            @Param("activo") Boolean activo,
            Pageable pageable);

}
