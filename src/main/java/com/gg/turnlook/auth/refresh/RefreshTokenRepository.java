package com.gg.turnlook.auth.refresh;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String hash);
    Optional<RefreshToken> findByUsuarioId(Integer usuarioId);
    @Modifying
    @Query("delete from RefreshToken t where t.usuario.id = :usuarioId")
    void revocarPorUsuario(Integer usuarioId);
}
