package com.gg.turnlook.features.verificacion;

import com.gg.turnlook.auth.BloqueoUsuario;
import com.gg.turnlook.auth.jwt.JwtService;
import com.gg.turnlook.common.exception.UnauthorizedException;
import com.gg.turnlook.features.usuario.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class VerificacionAccesoService {
    private final JwtService jwt;
    private final BloqueoUsuario bloqueo;

    @Transactional
    public Usuario validar(String email, String ticket) {
        Claims claims;
        try {
            claims = jwt.validarTokenVerificacion(ticket);
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException("Volvé a iniciar sesión para verificar tu cuenta");
        }
        Integer id = claims.get("id", Integer.class);
        if (id == null) throw new UnauthorizedException("Acceso de verificación inválido");
        Usuario usuario = bloqueo.bloquear(id);
        if (!usuario.isActivo() || !usuario.getEmail().equals(email.trim().toLowerCase(Locale.ROOT))
                || !usuario.getEmail().equals(claims.getSubject())
                || !Integer.valueOf(usuario.getCredencialesVersion()).equals(claims.get("version", Integer.class))) {
            throw new UnauthorizedException("Acceso de verificación inválido");
        }
        return usuario;
    }
}
