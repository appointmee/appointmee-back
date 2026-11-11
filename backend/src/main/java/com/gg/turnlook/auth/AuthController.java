package com.gg.turnlook.auth;
import com.gg.turnlook.auth.dto.*;
import com.gg.turnlook.auth.password.PasswordResetService;
import com.gg.turnlook.features.usuario.UsuarioService;
import com.gg.turnlook.features.usuario.dto.UsuarioCrearDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;
    private final UsuarioService usuarios;
    private final PasswordResetService passwords;
    private final com.gg.turnlook.features.verificacion.VerificacionEmailService verificacionEmail;
    @PostMapping({"/auth/login", "/usuarios/inicio-sesion"})
    public LoginResponseDTO login(@Valid @RequestBody LoginDTO request) { return auth.login(request); }
    @PostMapping({"/auth/register", "/usuarios/crear"})
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody UsuarioCrearDTO request) {
        usuarios.crearUsuario(request);
        enviarVerificacion(request.getEmail());
        return ResponseEntity.status(201).body(new MessageResponse("Se creó al usuario. Verificá tu email para iniciar sesión."));
    }
    @PostMapping("/auth/refresh")
    public LoginResponseDTO refresh(@Valid @RequestBody RefreshTokenRequest request) { return auth.renovar(request.refreshToken()); }
    @PostMapping("/auth/logout")
    public MessageResponse logout(@AuthenticationPrincipal String email) {
        auth.logout(email); return new MessageResponse("Sesión cerrada");
    }
    @PostMapping("/auth/forgot-password")
    public ResponseEntity<MessageResponse> forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        boolean permitido;
        try {
            permitido = passwords.solicitarCodigo(request.email());
        } catch (org.springframework.web.client.RestClientException e) {
            // Do not expose whether a recipient exists when the email provider fails.
            org.slf4j.LoggerFactory.getLogger(AuthController.class).warn("No se pudo enviar el correo de recuperación");
            return respuestaGenericaRecuperacion();
        }
        if (!permitido) {
            return ResponseEntity.status(429).body(new MessageResponse("Demasiadas solicitudes. Esperá un minuto e intentá nuevamente."));
        }
        return respuestaGenericaRecuperacion();
    }

    private ResponseEntity<MessageResponse> respuestaGenericaRecuperacion() {
        return ResponseEntity.ok(new MessageResponse("Si el email está registrado, te enviamos un código de 6 dígitos. Revisá también spam"));
    }
    @PostMapping("/auth/reset-password")
    public MessageResponse reset(@Valid @RequestBody ResetPasswordRequest request) {
        passwords.restablecer(request.token(), request.newPassword());
        return new MessageResponse("Contraseña restablecida. Iniciá sesión nuevamente.");
    }

    @PostMapping("/auth/reset-password/code")
    public ResponseEntity<MessageResponse> resetWithCode(@Valid @RequestBody ResetPasswordCodeRequest request) {
        if (!passwords.restablecerConCodigo(request.email(), request.code(), request.newPassword())) {
            return ResponseEntity.badRequest().body(new MessageResponse("El código es inválido, venció o se agotaron los intentos."));
        }
        return ResponseEntity.ok(new MessageResponse("Contraseña restablecida. Iniciá sesión nuevamente."));
    }

    @PostMapping("/auth/reset-password/validate")
    public ResponseEntity<EstadoRecuperacionResponse> validarRecuperacion(
            @Valid @RequestBody PasswordResetTokenRequest request) {
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(passwords.validarEnlace(request.token()));
    }
    @PostMapping("/auth/change-password")
    public MessageResponse change(@AuthenticationPrincipal String email, @Valid @RequestBody ChangePasswordRequest request) {
        auth.cambiarPassword(email, request);
        return new MessageResponse("Contraseña modificada. Iniciá sesión nuevamente.");
    }

    @PostMapping("/auth/verification-status")
    public EstadoVerificacionResponse estadoVerificacion(@Valid @RequestBody ForgotPasswordRequest request,
            @RequestHeader(value = "X-Verification-Token", required = false) String ticket) {
        return verificacionEmail.consultarEstado(request.email(), ticket);
    }

    @PostMapping("/auth/send-verification")
    public ResponseEntity<?> solicitarVerificacion(@Valid @RequestBody SolicitarVerificacionEmailRequest request,
            @RequestHeader(value = "X-Verification-Token", required = false) String ticket) {
        try {
            return ResponseEntity.accepted().body(verificacionEmail.solicitarConAcceso(request.email(), ticket, Boolean.TRUE.equals(request.reenviar())));
        } catch (org.springframework.web.client.RestClientException e) {
            return ResponseEntity.status(503).body(new MessageResponse("No pudimos enviar el código. Intentá nuevamente."));
        }
    }

    @PostMapping("/auth/verify-email")
    public VerificacionEmailResponse verificarEmail(@Valid @RequestBody VerificarEmailRequest request,
            @RequestHeader(value = "X-Verification-Token", required = false) String ticket) {
        return auth.verificarEmail(request.email(), request.codigo(), ticket);
    }

    @PostMapping("/auth/complete-verification")
    public VerificacionEmailResponse completarVerificacion(@Valid @RequestBody ForgotPasswordRequest request,
            @RequestHeader(value = "X-Verification-Token", required = false) String ticket) {
        return auth.completarVerificacion(request.email(), ticket);
    }

    private void enviarVerificacion(String email) {
        try {
            verificacionEmail.solicitar(email);
        } catch (org.springframework.web.client.RestClientException e) {
            org.slf4j.LoggerFactory.getLogger(AuthController.class).warn("No se pudo enviar el correo de verificación");
        }
    }
}
