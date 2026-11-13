package com.gg.turnlook.features.usuario;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gg.turnlook.auth.BloqueoUsuario;
import com.gg.turnlook.auth.password.PasswordResetTokenRepository;
import com.gg.turnlook.auth.refresh.RefreshTokenRepository;
import com.gg.turnlook.features.imagen.ImagenService;
import com.gg.turnlook.features.resenia.ReseniaService;
import com.gg.turnlook.features.usuario.rol.RolService;
import com.gg.turnlook.features.verificacion.VerificacionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class UsuarioServiceActivacionTest {
    @Test
    void activaUnaCuentaInactivaBloqueandoElRegistro() {
        UsuarioRepository repository = mock(UsuarioRepository.class);
        BloqueoUsuario bloqueo = mock(BloqueoUsuario.class);
        UsuarioService service = new UsuarioService(
                repository,
                mock(RolService.class),
                mock(ImagenService.class),
                mock(ReseniaService.class),
                mock(PasswordEncoder.class),
                mock(RefreshTokenRepository.class),
                mock(PasswordResetTokenRepository.class),
                bloqueo,
                mock(VerificacionRepository.class));
        Usuario usuario = new Usuario("Mario", "Paz", "mario@example.com", "hash");
        usuario.setId(72);
        usuario.setActivo(false);
        when(bloqueo.bloquear(72)).thenReturn(usuario);

        service.activarUsuario(72);

        assertTrue(usuario.isActivo());
        verify(repository).save(usuario);
    }
}
