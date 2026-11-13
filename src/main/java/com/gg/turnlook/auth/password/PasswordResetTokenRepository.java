package com.gg.turnlook.auth.password;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String hash);
    Optional<PasswordResetToken> findFirstByUsuarioIdOrderByFechaVencimientoDesc(Integer usuarioId);
    
    @Modifying
    @Query("delete from PasswordResetToken t where t.usuario.id = :usuarioId")
    void revocarPorUsuario(Integer usuarioId);
}
