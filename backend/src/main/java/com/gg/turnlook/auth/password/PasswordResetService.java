package com.gg.turnlook.auth.password;
import com.gg.turnlook.auth.TokenSeguro;
import com.gg.turnlook.auth.dto.EstadoRecuperacionResponse;
import com.gg.turnlook.auth.refresh.RefreshTokenRepository;
import com.gg.turnlook.common.exception.BadRequestException;
import com.gg.turnlook.features.usuario.*;
import com.gg.turnlook.features.verificacion.canalEnvio.EmailService;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor
public class PasswordResetService {
    private static final int MAX_INTENTOS_CODIGO = 5;
    private static final long ESPERA_SOLICITUD_SEGUNDOS = 60;
    private final ConcurrentHashMap<String, Instant> solicitudesRecientes = new ConcurrentHashMap<>();
    private final AtomicLong solicitudes = new AtomicLong();
    private final PasswordResetTokenRepository repository;
    private final RefreshTokenRepository refreshTokens;
    private final UsuarioRepository usuarios;
    private final com.gg.turnlook.auth.BloqueoUsuario bloqueo;
    private final PasswordEncoder encoder;
    private final EmailService email;
    private final org.springframework.context.ApplicationEventPublisher events;
    @Value("${auth.password-reset-ttl-seconds:900}") private long ttl;
    @Value("${auth.password-reset-url:http://localhost:4200/reset-password}") private String resetUrl;

    /** Devuelve false para el mismo límite por email exista o no una cuenta. */
    @Transactional
    public boolean solicitarCodigo(String destinatario) {
        String emailNormalizado = destinatario.trim().toLowerCase(Locale.ROOT);
        Instant ahora = Instant.now();
        if (solicitudes.incrementAndGet() % 256 == 0) {
            solicitudesRecientes.entrySet().removeIf(entry -> entry.getValue().plusSeconds(ESPERA_SOLICITUD_SEGUNDOS).isBefore(ahora));
        }
        if (solicitudesRecientes.size() >= 10_000 && !solicitudesRecientes.containsKey(emailNormalizado)) return false;
        Instant anterior = solicitudesRecientes.putIfAbsent(emailNormalizado, ahora);
        if (anterior != null) {
            if (ahora.isBefore(anterior.plusSeconds(ESPERA_SOLICITUD_SEGUNDOS))) return false;
            if (!solicitudesRecientes.replace(emailNormalizado, anterior, ahora)) return false;
        }

        usuarios.findByEmailAndActivoTrue(emailNormalizado).ifPresent(encontrado -> {
            Usuario usuario = bloqueo.bloquear(encontrado.getId());
            if (!usuario.isActivo()) return;
            repository.revocarPorUsuario(usuario.getId());
            String codigo = TokenSeguro.generarCodigo();
            PasswordResetToken registro = new PasswordResetToken();
            registro.setUsuario(usuario);
            // Los códigos tienen solo un millón de combinaciones: se usa el hash lento
            // del encoder para que una filtración de la tabla no permita probarlos offline.
            registro.setTokenHash(encoder.encode(codigo));
            registro.setFechaVencimiento(ahora.plusSeconds(ttl));
            repository.save(registro);
            email.enviarCodigoRecuperacion(usuario.getEmail(), codigo, ttl / 60);
        });
        return true;
    }

    @Transactional
    public boolean restablecerConCodigo(String emailIngresado, String codigo, String password) {
        String emailNormalizado = emailIngresado.trim().toLowerCase(Locale.ROOT);
        Usuario encontrado = usuarios.findByEmailAndActivoTrue(emailNormalizado).orElse(null);
        if (encontrado == null) return false;
        Usuario usuario = bloqueo.bloquear(encontrado.getId());
        PasswordResetToken token = repository.findFirstByUsuarioIdOrderByFechaVencimientoDesc(usuario.getId()).orElse(null);
        if (token == null || token.getFechaUso() != null || !Instant.now().isBefore(token.getFechaVencimiento())
                || token.getIntentosFallidos() >= MAX_INTENTOS_CODIGO) return false;

        boolean coincide = encoder.matches(codigo, token.getTokenHash());
        if (!coincide) {
            int intentosFallidos = token.getIntentosFallidos() + 1;
            token.setIntentosFallidos(intentosFallidos);
            if (intentosFallidos >= MAX_INTENTOS_CODIGO) token.setFechaUso(Instant.now());
            repository.save(token);
            return false;
        }

        usuario.setPassword(encoder.encode(password));
        usuario.setCredencialesVersion(usuario.getCredencialesVersion() + 1);
        token.setFechaUso(Instant.now());
        refreshTokens.revocarPorUsuario(usuario.getId());
        repository.revocarPorUsuario(usuario.getId());
        events.publishEvent(new PasswordChangedEvent(usuario.getEmail()));
        return true;
    }
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
