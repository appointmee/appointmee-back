package com.gg.turnlook.features.verificacion;

import com.gg.turnlook.auth.BloqueoUsuario;
import com.gg.turnlook.common.exception.BadRequestException;
import com.gg.turnlook.common.exception.CodigoVerificacionException;
import com.gg.turnlook.auth.dto.EstadoVerificacionResponse;
import com.gg.turnlook.features.usuario.Usuario;
import com.gg.turnlook.features.usuario.UsuarioRepository;
import com.gg.turnlook.features.verificacion.canalEnvio.EmailService;
import com.gg.turnlook.features.verificacion.enums.CanalVerificacion;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VerificacionEmailService {

    private final UsuarioRepository usuarios;
    private final VerificacionAccesoService acceso;
    private final VerificacionRepository verificaciones;
    private final BloqueoUsuario bloqueo;
    private final PasswordEncoder encoder;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();


    @Transactional
    public EstadoVerificacionResponse consultarEstado(String email, String ticket) {
        return estado(acceso.validar(email, ticket), false);
    }

    @Transactional
    public EstadoVerificacionResponse solicitarConAcceso(String email, String ticket, boolean reenviar) {
        Usuario usuario = acceso.validar(email, ticket);
        EstadoVerificacionResponse actual = estado(usuario, false);
        if (actual.verificada() || actual.retryAfterSeconds() > 0) return actual;
        // Recargar o volver al flujo no reemplaza un código que sigue vigente.
        if (!reenviar && actual.enviado()) return actual;
        solicitar(email);
        return estado(usuario, true);
    }

    @Transactional(noRollbackFor = CodigoVerificacionException.class)
    public void confirmarConAcceso(String email, String codigo, String ticket) {
        acceso.validar(email, ticket);
        confirmar(email, codigo);
    }

    private EstadoVerificacionResponse estado(Usuario usuario, boolean enviadoAhora) {
        var ultimo = verificaciones.findFirstByUsuarioIdAndCanalAndDestinatarioOrderByFechaCreacionDescIdDesc(
                usuario.getId(), CanalVerificacion.EMAIL, usuario.getEmail());
        if (usuario.isEmailVerificado()) return new EstadoVerificacionResponse(true, false, false, 0, 0, 0);
        if (ultimo.isEmpty() || !usuario.getEmail().equals(ultimo.get().getDestinatario())) {
            return new EstadoVerificacionResponse(false, false, false, 0, 5, 0);
        }
        Verificacion registro = ultimo.get();
        long cantidad = verificaciones.countByUsuarioIdAndCanalAndDestinatario(usuario.getId(), CanalVerificacion.EMAIL, usuario.getEmail());
        long espera = cantidad <= 1 ? 15 : 60;
        java.time.Instant ahora = java.time.Instant.now();
        long restante = Math.max(0, (java.time.Duration.between(ahora, registro.getFechaCreacion().plusSeconds(espera)).toMillis() + 999) / 1000);
        long vigencia = Math.max(0, (java.time.Duration.between(ahora, registro.getFechaVencimiento()).toMillis() + 999) / 1000);
        int intentos = Math.max(0, 5 - registro.getIntentosFallidos());
        boolean enviado = registro.getFechaUso() == null && vigencia > 0 && intentos > 0;
        return new EstadoVerificacionResponse(false, enviado, enviadoAhora, restante, intentos, vigencia);
    }

    private CodigoVerificacionException fallo(Usuario usuario, String codigo) {
        return new CodigoVerificacionException(codigo, estado(usuario, false));
    }

    @Transactional
    public void solicitar(String email) {
        String destinatario = normalizar(email);
        var encontrado = usuarios.findByEmailAndActivoTrue(destinatario);
        if (encontrado.isEmpty()) return;

        Usuario usuario = bloqueo.bloquear(encontrado.get().getId());
        if (!usuario.isActivo() || usuario.isEmailVerificado()
                || !destinatario.equals(usuario.getEmail())) return;

        Instant ahora = Instant.now();
        var ultima = verificaciones.findFirstByUsuarioIdAndCanalAndDestinatarioOrderByFechaCreacionDescIdDesc(
                usuario.getId(), CanalVerificacion.EMAIL, usuario.getEmail());

        long envios = verificaciones.countByUsuarioIdAndCanalAndDestinatario(
                usuario.getId(), CanalVerificacion.EMAIL, destinatario);
        long espera = envios <= 1 ? 15 : 60;

        // Primer reenvío a los 15 segundos; los siguientes a los 60.
        if (ultima.isPresent() && ahora.isBefore(ultima.get().getFechaCreacion().plusSeconds(espera))) return;

        verificaciones.invalidarPorUsuario(usuario.getId(), ahora);
        String codigo;
        do {
            codigo = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        } while (ultima.isPresent() && encoder.matches(codigo, ultima.get().getCodigoHash()));
        verificaciones.save(Verificacion.builder()
                .usuario(usuario)
                .canal(CanalVerificacion.EMAIL)
                .destinatario(destinatario)
                .codigoHash(encoder.encode(codigo))
                .fechaCreacion(ahora)
                .fechaVencimiento(ahora.plusSeconds(300))
                .intentosFallidos(0)
                .build());
        emailService.enviarCodigo(destinatario, codigo);
    }


    // Persistir los intentos incorrectos aunque se responda 400.
    @Transactional(noRollbackFor = CodigoVerificacionException.class)
    public void confirmar(String email, String codigo) {
        String destinatario = normalizar(email);
        Usuario encontrado = usuarios.findByEmailAndActivoTrue(destinatario)
                .orElseThrow(VerificacionEmailService::codigoInvalido);
        Usuario usuario = bloqueo.bloquear(encontrado.getId());
        if (!usuario.isActivo() || !destinatario.equals(usuario.getEmail())) throw codigoInvalido();
        if (usuario.isEmailVerificado()) throw fallo(usuario, "EMAIL_ALREADY_VERIFIED");

        Verificacion verificacion = verificaciones
                .findFirstByUsuarioIdAndCanalAndDestinatarioOrderByFechaCreacionDescIdDesc(usuario.getId(), CanalVerificacion.EMAIL, usuario.getEmail())
                .orElseThrow(VerificacionEmailService::codigoInvalido);
        Instant ahora = Instant.now();
        if (!destinatario.equals(verificacion.getDestinatario()) || verificacion.getFechaUso() != null)
            throw fallo(usuario, "VERIFICATION_CODE_INVALID");
        if (verificacion.getIntentosFallidos() >= 5)
            throw fallo(usuario, "VERIFICATION_ATTEMPTS_EXCEEDED");
        if (!ahora.isBefore(verificacion.getFechaVencimiento()))
            throw fallo(usuario, "VERIFICATION_CODE_EXPIRED");

        if (!encoder.matches(codigo, verificacion.getCodigoHash())) {
            verificacion.setIntentosFallidos(verificacion.getIntentosFallidos() + 1);
            throw fallo(usuario, verificacion.getIntentosFallidos() >= 5
                    ? "VERIFICATION_ATTEMPTS_EXCEEDED" : "VERIFICATION_CODE_INVALID");
        }
        usuario.setEmailVerificado(true);
        verificacion.setFechaUso(ahora);
        verificaciones.invalidarPorUsuario(usuario.getId(), ahora);
    }


    private static String normalizar(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static BadRequestException codigoInvalido() {
        return new BadRequestException("Código inválido, vencido o utilizado");
    }
}
