package com.gg.turnlook.features.usuario;

import com.gg.turnlook.auth.BloqueoUsuario;
import com.gg.turnlook.common.exception.BadRequestException;
import com.gg.turnlook.common.exception.ConflictException;
import com.gg.turnlook.common.exception.UnauthorizedException;
import com.gg.turnlook.features.verificacion.Verificacion;
import com.gg.turnlook.features.verificacion.VerificacionRepository;
import com.gg.turnlook.features.verificacion.canalEnvio.EmailService;
import com.gg.turnlook.features.verificacion.enums.CanalVerificacion;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class CambioCorreoService {
    private static final Logger LOG = LoggerFactory.getLogger(CambioCorreoService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    // El prefijo separa los códigos de cambio de los de verificación de cuenta.
    private static final String PREFIJO = "cambio-correo:";

    private final UsuarioRepository usuarios;
    private final UsuarioService usuarioService;
    private final BloqueoUsuario bloqueo;
    private final VerificacionRepository verificaciones;
    private final PasswordEncoder encoder;
    private final EmailService correo;

    @Transactional
    public void solicitar(String correoActual, String nuevo) {
        Usuario usuario = bloquear(correoActual);
        String destino = validarDestino(usuario, nuevo);
        Instant ahora = Instant.now();
        var ultima = verificaciones.findFirstByUsuarioIdAndCanalOrderByFechaCreacionDescIdDesc(
                usuario.getId(), CanalVerificacion.EMAIL);
        if (ultima.isPresent() && ahora.isBefore(ultima.get().getFechaCreacion().plusSeconds(60))) {
            throw new BadRequestException("Esperá un minuto antes de solicitar otro código.");
        }
        verificaciones.invalidarPorUsuario(usuario.getId(), ahora);
        String codigo = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
        verificaciones.save(Verificacion.builder()
                .usuario(usuario)
                .canal(CanalVerificacion.EMAIL)
                .destinatario(PREFIJO + destino)
                .codigoHash(encoder.encode(codigo))
                .fechaCreacion(ahora)
                .fechaVencimiento(ahora.plusSeconds(300))
                .intentosFallidos(0)
                .build());
        correo.enviarCodigoCambioCorreo(destino, codigo);
    }

    @Transactional(noRollbackFor = BadRequestException.class)
    public void confirmar(String correoActual, String nuevo, String codigo) {
        Usuario usuario = bloquear(correoActual);
        String destino = validarDestino(usuario, nuevo);
        Instant ahora = Instant.now();
        Verificacion solicitud = verificaciones
                .findFirstByUsuarioIdAndCanalAndDestinatarioOrderByFechaCreacionDescIdDesc(
                        usuario.getId(), CanalVerificacion.EMAIL, PREFIJO + destino)
                .orElseThrow(CambioCorreoService::codigoInvalido);
        if (solicitud.getFechaUso() != null || !ahora.isBefore(solicitud.getFechaVencimiento())
                || solicitud.getIntentosFallidos() >= 5) {
            throw codigoInvalido();
        }
        if (!encoder.matches(codigo, solicitud.getCodigoHash())) {
            solicitud.setIntentosFallidos(solicitud.getIntentosFallidos() + 1);
            throw codigoInvalido();
        }
        String anterior = usuario.getEmail();
        usuario.setEmail(destino);
        usuario.setEmailVerificado(true);
        try {
            usuarios.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException error) {
            throw new ConflictException("El correo ingresado ya está en uso.");
        }
        usuarioService.revocarSesiones(usuario);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    correo.enviarAvisoCambioCorreo(anterior);
                } catch (RuntimeException error) {
                    LOG.warn("No se pudo enviar el aviso de cambio de correo para el usuario {}", usuario.getId());
                }
            }
        });
    }

    private Usuario bloquear(String email) {
        Usuario encontrado = usuarios.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new UnauthorizedException("Usuario no disponible"));
        Usuario usuario = bloqueo.bloquear(encontrado.getId());
        if (!usuario.isActivo() || !email.equals(usuario.getEmail())) {
            throw new UnauthorizedException("Usuario no disponible");
        }
        return usuario;
    }

    private String validarDestino(Usuario usuario, String nuevo) {
        String destino = nuevo.trim().toLowerCase(Locale.ROOT);
        if (usuario.getEmail().equals(destino)) {
            throw new BadRequestException("Ingresá un correo distinto al actual.");
        }
        if (usuarios.existsByEmail(destino)) {
            throw new ConflictException("El correo ingresado ya está en uso.");
        }
        return destino;
    }

    private static BadRequestException codigoInvalido() {
        return new BadRequestException("Código inválido, vencido o agotado. Podés pedir uno nuevo.");
    }
}
