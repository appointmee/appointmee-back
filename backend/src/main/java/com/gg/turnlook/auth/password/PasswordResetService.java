package com.gg.turnlook.auth.password;
import com.gg.turnlook.auth.TokenSeguro;
import com.gg.turnlook.auth.dto.EstadoRecuperacionResponse;
import com.gg.turnlook.auth.refresh.RefreshTokenRepository;
import com.gg.turnlook.common.exception.BadRequestException;
import com.gg.turnlook.features.usuario.*;
import com.gg.turnlook.features.verificacion.canalEnvio.EmailService;
import java.time.Instant;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor
public class PasswordResetService {
    private final PasswordResetTokenRepository repository;
    private final RefreshTokenRepository refreshTokens;
    private final UsuarioRepository usuarios;
    private final com.gg.turnlook.auth.BloqueoUsuario bloqueo;
    private final PasswordEncoder encoder;
    private final EmailService email;
    private final org.springframework.context.ApplicationEventPublisher events;
    @Value("${auth.password-reset-ttl-seconds:900}") private long ttl;
    @Value("${auth.password-reset-url:http://localhost:4200/reset-password}") private String resetUrl;
    @Transactional
    public void solicitar(String destinatario) {
        usuarios.findByEmailAndActivoTrue(destinatario.trim().toLowerCase(Locale.ROOT)).ifPresent(encontrado -> {
            Usuario usuario = bloqueo.bloquear(encontrado.getId());
            if (!usuario.isActivo()) return;
            // Quiet cooldown: the public response stays the same for every email.
            if (repository.findFirstByUsuarioIdOrderByFechaVencimientoDesc(usuario.getId())
                    .filter(t -> Instant.now().isBefore(t.getFechaVencimiento().minusSeconds(ttl).plusSeconds(60)))
                    .isPresent()) return;
            repository.revocarPorUsuario(usuario.getId());
            String token = TokenSeguro.generar();
            PasswordResetToken registro = new PasswordResetToken();
            registro.setUsuario(usuario);
            registro.setTokenHash(TokenSeguro.hash(token));
            registro.setFechaVencimiento(Instant.now().plusSeconds(ttl));
            repository.save(registro);
            email.enviarRecuperacion(usuario.getEmail(), resetUrl + (resetUrl.contains("?") ? "&" : "?") + "token=" + token, ttl / 60);
        });
    }
    @Transactional
    public void restablecer(String token, String password) {
        String hash = TokenSeguro.hash(token);
        PasswordResetToken encontrado = repository.findByTokenHash(hash).orElseThrow(PasswordResetService::invalido);
        Usuario usuario = bloqueo.bloquear(encontrado.getUsuario().getId());
        PasswordResetToken actual = repository.findByTokenHash(hash).orElseThrow(PasswordResetService::invalido);
        if (!usuario.isActivo() || actual.getFechaUso() != null || !Instant.now().isBefore(actual.getFechaVencimiento())) throw invalido();
        usuario.setPassword(encoder.encode(password));
        usuario.setCredencialesVersion(usuario.getCredencialesVersion() + 1);
        refreshTokens.revocarPorUsuario(usuario.getId());
        repository.revocarPorUsuario(usuario.getId());
        events.publishEvent(new PasswordChangedEvent(usuario.getEmail()));
    }

    @Transactional(readOnly = true)
    public EstadoRecuperacionResponse validarEnlace(String token) {
        return repository.findByTokenHash(TokenSeguro.hash(token))
                .filter(registro -> registro.getFechaUso() == null && registro.getUsuario().isActivo())
                .map(registro -> {
                    long segundos = java.time.Duration.between(Instant.now(), registro.getFechaVencimiento()).toSeconds();
                    return new EstadoRecuperacionResponse(segundos > 0, Math.max(0, segundos));
                })
                .orElseGet(() -> new EstadoRecuperacionResponse(false, 0));
    }
    private static BadRequestException invalido() { return new BadRequestException("Token inválido, vencido o utilizado"); }
}
