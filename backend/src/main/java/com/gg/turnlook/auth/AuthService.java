package com.gg.turnlook.auth;
import com.gg.turnlook.auth.dto.*;
import com.gg.turnlook.auth.jwt.JwtService;
import com.gg.turnlook.auth.refresh.*;
import com.gg.turnlook.auth.password.PasswordResetTokenRepository;
import com.gg.turnlook.common.exception.UnauthorizedException;
import com.gg.turnlook.features.usuario.*;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarios;
    private final com.gg.turnlook.auth.BloqueoUsuario bloqueo;
    private final JwtService jwt;
    private final RefreshTokenService refresh;
    private final RefreshTokenRepository refreshRepository;
    private final PasswordResetTokenRepository resetRepository;
    private final PasswordEncoder encoder;
    private final com.gg.turnlook.features.verificacion.VerificacionEmailService verificacionEmail;
    private final com.gg.turnlook.features.verificacion.VerificacionAccesoService accesoVerificacion;
    @Transactional
    public LoginResponseDTO login(LoginDTO request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        // Consulta anterior a la optimización. Reemplazada por findIdByEmailAndActivoTrue.
        // Usuario encontrado = usuarios.findByEmailAndActivoTrue(email).orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas"));
        // Usuario usuario = bloqueo.bloquear(encontrado.getId());
        Integer usuarioId = usuarios.findIdByEmailAndActivoTrue(email)
                .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas"));
        Usuario usuario = bloqueo.bloquear(usuarioId);
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.getPass()));
        if (!usuario.isActivo()) throw new BadCredentialsException("Credenciales incorrectas");

        // Informar el estado de verificación solo después de autenticar la contraseña.
        if (!usuario.isEmailVerificado()) {
            throw new com.gg.turnlook.common.exception.EmailNoVerificadoException(jwt.generarTokenVerificacion(usuario));
        }
        return tokens(usuario);
    }
    @Transactional(noRollbackFor = com.gg.turnlook.common.exception.CodigoVerificacionException.class)
    public VerificacionEmailResponse verificarEmail(String email, String codigo, String ticket) {
        verificacionEmail.confirmarConAcceso(email, codigo, ticket);
        return completarVerificacion(email, ticket);
    }

    @Transactional
    public VerificacionEmailResponse completarVerificacion(String email, String ticket) {
        Usuario usuario = accesoVerificacion.validar(email, ticket);
        if (!usuario.isEmailVerificado()) throw new com.gg.turnlook.common.exception.BadRequestException("La cuenta todavía no está verificada");
        LoginResponseDTO sesion = tokens(usuario);

        var perfil = new com.gg.turnlook.features.usuario.dto.UsuarioMeDTO(
                usuario.getId(),
                usuario.getRoles().stream().map(r -> r.getRol().name()).collect(java.util.stream.Collectors.toSet()),
                usuario.getFotoPerfil() == null ? null : usuario.getFotoPerfil().getFotoValida());

        return new VerificacionEmailResponse("Tu cuenta fue verificada correctamente.",
                sesion.getToken(), sesion.getToken(), sesion.getRefreshToken(), sesion.getExpiresIn(), perfil);
    }

    @Transactional
    public LoginResponseDTO renovar(String token) { return tokens(refresh.validar(token)); }
    private LoginResponseDTO tokens(Usuario usuario) {
        return new LoginResponseDTO(jwt.generarToken(usuario), refresh.emitir(usuario), jwt.getTtlSeconds());
    }
    @Transactional
    public void logout(String email) {
        Usuario usuario = bloquear(email);
        refreshRepository.revocarPorUsuario(usuario.getId());
    }
    @Transactional
    public void cambiarPassword(String email, ChangePasswordRequest request) {
        Usuario usuario = bloquear(email);
        if (!encoder.matches(request.currentPassword(), usuario.getPassword())) throw new UnauthorizedException("Contraseña actual incorrecta");
        usuario.setPassword(encoder.encode(request.newPassword()));
        usuario.setCredencialesVersion(usuario.getCredencialesVersion() + 1);
        refreshRepository.revocarPorUsuario(usuario.getId());
        resetRepository.revocarPorUsuario(usuario.getId());
    }
    private Usuario bloquear(String email) {
        Usuario usuario = usuarios.findByEmailAndActivoTrue(email).orElseThrow(() -> new UnauthorizedException("Sesión inválida"));
        return bloqueo.bloquear(usuario.getId());
    }
}
