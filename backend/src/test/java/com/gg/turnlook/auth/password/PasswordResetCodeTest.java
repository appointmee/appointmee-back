package com.gg.turnlook.auth.password;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gg.turnlook.auth.BloqueoUsuario;
import com.gg.turnlook.auth.refresh.RefreshTokenRepository;
import com.gg.turnlook.features.usuario.Usuario;
import com.gg.turnlook.features.usuario.UsuarioRepository;
import com.gg.turnlook.features.verificacion.canalEnvio.EmailService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordResetCodeTest {
    private final PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
    private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final BloqueoUsuario bloqueo = mock(BloqueoUsuario.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final EmailService email = mock(EmailService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final Usuario usuario = mock(Usuario.class);
    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(tokens, refreshTokens, usuarios, bloqueo, encoder, email, events);
        when(usuarios.findByEmailAndActivoTrue("persona@example.com")).thenReturn(Optional.of(usuario));
        when(usuario.getId()).thenReturn(20);
        when(usuario.getEmail()).thenReturn("persona@example.com");
        when(usuario.isActivo()).thenReturn(true);
        when(bloqueo.bloquear(20)).thenReturn(usuario);
        when(encoder.encode(anyString())).thenReturn("password-hash");
    }

    @Test
    void solicitudDesconocidaAvanzaConRespuestaEquivalenteYAplicaCooldown() {
        when(usuarios.findByEmailAndActivoTrue("nadie@example.com")).thenReturn(Optional.empty());
        assertTrue(service.solicitarCodigo("nadie@example.com"));
        assertFalse(service.solicitarCodigo("NADIE@example.com"));
        verify(email, never()).enviarCodigoRecuperacion(anyString(), anyString(), anyLong());
    }

    @Test
    void generaCodigoDeSeisDigitosYGuardaSoloSuHash() {
        when(tokens.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertTrue(service.solicitarCodigo(" Persona@Example.com "));

        ArgumentCaptor<PasswordResetToken> registro = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokens).save(registro.capture());
        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(email).enviarCodigoRecuperacion(anyString(), codigo.capture(), anyLong());
        org.junit.jupiter.api.Assertions.assertTrue(codigo.getValue().matches("\\d{6}"));
        verify(encoder).encode(codigo.getValue());
        org.junit.jupiter.api.Assertions.assertNotEquals(codigo.getValue(), registro.getValue().getTokenHash());
    }

    @Test
    void codigoIncorrectoIncrementaIntentosYAlQuintoLoInvalida() {
        PasswordResetToken token = mock(PasswordResetToken.class);
        when(token.getFechaUso()).thenReturn(null);
        when(token.getFechaVencimiento()).thenReturn(Instant.now().plusSeconds(300));
        when(token.getIntentosFallidos()).thenReturn(4);
        when(token.getTokenHash()).thenReturn("otp-hash");
        when(encoder.matches("654321", "otp-hash")).thenReturn(false);
        when(tokens.findFirstByUsuarioIdOrderByFechaVencimientoDesc(20)).thenReturn(Optional.of(token));

        assertFalse(service.restablecerConCodigo("persona@example.com", "654321", "NuevaClave123!"));
        verify(token).setIntentosFallidos(5);
        verify(token).setFechaUso(any(Instant.class));
        verify(usuario, never()).setPassword(anyString());
    }

    @Test
    void codigoValidoActualizaCredencialesRevocaSesionesYConsumeCodigo() {
        PasswordResetToken token = mock(PasswordResetToken.class);
        when(token.getFechaUso()).thenReturn(null);
        when(token.getFechaVencimiento()).thenReturn(Instant.now().plusSeconds(300));
        when(token.getIntentosFallidos()).thenReturn(0);
        when(token.getTokenHash()).thenReturn("otp-hash");
        when(encoder.matches("012345", "otp-hash")).thenReturn(true);
        when(tokens.findFirstByUsuarioIdOrderByFechaVencimientoDesc(20)).thenReturn(Optional.of(token));
        when(usuario.getCredencialesVersion()).thenReturn(1);

        assertTrue(service.restablecerConCodigo(" PERSONA@example.com ", "012345", "NuevaClave123!"));
        verify(usuario).setPassword("password-hash");
        verify(usuario).setCredencialesVersion(2);
        verify(refreshTokens).revocarPorUsuario(20);
        verify(tokens).revocarPorUsuario(20);
        verify(token).setFechaUso(any(Instant.class));
    }
}
