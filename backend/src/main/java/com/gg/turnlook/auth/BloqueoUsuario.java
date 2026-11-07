package com.gg.turnlook.auth;
import com.gg.turnlook.features.usuario.*;
import com.gg.turnlook.common.exception.UnauthorizedException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class BloqueoUsuario {
    private final UsuarioRepository usuarios;
    private final EntityManager entityManager;
    // Refresh an already managed entity after acquiring the lock, avoiding stale credentials.
    public Usuario bloquear(Integer id) {
        Usuario usuario = usuarios.bloquearPorId(id).orElseThrow(() -> new UnauthorizedException("Usuario no disponible"));
        entityManager.refresh(usuario);
        return usuario;
    }
}
