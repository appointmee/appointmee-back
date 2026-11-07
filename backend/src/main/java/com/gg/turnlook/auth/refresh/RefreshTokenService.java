package com.gg.turnlook.auth.refresh;
import com.gg.turnlook.auth.TokenSeguro;
import com.gg.turnlook.common.exception.UnauthorizedException;
import com.gg.turnlook.features.usuario.*;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
@Service @RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository repository;
    private final com.gg.turnlook.auth.BloqueoUsuario bloqueo;
    @Value("${auth.refresh-ttl-seconds:604800}") private long ttl;
    // All writes lock the user first, serializing rotation, login and revocation.
    @Transactional
    public String emitir(Usuario usuario) {
        bloqueo.bloquear(usuario.getId());
        RefreshToken registro = repository.findByUsuarioId(usuario.getId()).orElseGet(RefreshToken::new);
        String token = TokenSeguro.generar();
        registro.setUsuario(usuario);
        registro.setTokenHash(TokenSeguro.hash(token));
        registro.setFechaVencimiento(Instant.now().plusSeconds(ttl));
        repository.save(registro);
        return token;
    }
    @Transactional
    public Usuario validar(String token) {
        String hash = TokenSeguro.hash(token);
        RefreshToken encontrado = repository.findByTokenHash(hash).orElseThrow(RefreshTokenService::invalido);
        Usuario usuario = bloqueo.bloquear(encontrado.getUsuario().getId());
        // Re-query by hash after the user lock: a concurrent rotation may have consumed it.
        RefreshToken actual = repository.findByTokenHash(hash).orElseThrow(RefreshTokenService::invalido);
        if (!usuario.isActivo() || !usuario.isEmailVerificado() || !Instant.now().isBefore(actual.getFechaVencimiento())) throw invalido();
        return usuario;
    }
    public static UnauthorizedException invalido() { return new UnauthorizedException("Refresh token inválido o vencido"); }
}
