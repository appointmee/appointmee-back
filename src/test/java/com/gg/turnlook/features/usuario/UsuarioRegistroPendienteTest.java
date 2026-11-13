package com.gg.turnlook.features.usuario;

import com.gg.turnlook.common.exception.ConflictException;
import com.gg.turnlook.common.exception.GlobalExceptionHandler;
import com.gg.turnlook.common.exception.RegistroPendienteVerificacionException;
import com.gg.turnlook.features.usuario.dto.UsuarioCrearDTO;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsuarioRegistroPendienteTest {
    @Test
    void informaEstadoPendienteSoloConPasswordCorrectaSinCrearOtroUsuario() {
        UsuarioRepository repo = mock(UsuarioRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        Usuario usuario = new Usuario();
        usuario.setPassword("hash");
        when(repo.existsByEmail("ana@example.com")).thenReturn(true);
        when(repo.findByEmailAndActivoTrue("ana@example.com")).thenReturn(Optional.of(usuario));
        when(encoder.matches("Clave123!", "hash")).thenReturn(true);
        UsuarioService service = new UsuarioService(repo, null, null, null, encoder, null, null, null, null);

        RegistroPendienteVerificacionException error = assertThrows(
                RegistroPendienteVerificacionException.class, () -> service.crearUsuario(datos()));
        var response = new GlobalExceptionHandler().handleConflict(error);
        assertEquals(409, response.getStatusCode().value());
        assertEquals("REGISTRATION_PENDING_VERIFICATION", ((Map<?, ?>) response.getBody()).get("code"));
        verify(repo, never()).save(any());
        assertFalse(usuario.isEmailVerificado());
    }

    @Test
    void passwordIncorrectaNoRevelaQueLaCuentaEstaPendiente() {
        UsuarioRepository repo = mock(UsuarioRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(repo.existsByEmail("ana@example.com")).thenReturn(true);
        when(repo.findByEmailAndActivoTrue("ana@example.com")).thenReturn(Optional.of(new Usuario()));
        UsuarioService service = new UsuarioService(repo, null, null, null, encoder, null, null, null, null);

        ConflictException error = assertThrows(ConflictException.class, () -> service.crearUsuario(datos()));
        assertEquals(ConflictException.class, error.getClass());
        verify(repo, never()).save(any());
    }

    @Test
    void cuentaYaVerificadaMantieneElConflictoGenerico() {
        UsuarioRepository repo = mock(UsuarioRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        Usuario usuario = new Usuario();
        usuario.setEmailVerificado(true);
        when(repo.existsByEmail("ana@example.com")).thenReturn(true);
        when(repo.findByEmailAndActivoTrue("ana@example.com")).thenReturn(Optional.of(usuario));
        UsuarioService service = new UsuarioService(repo, null, null, null, encoder, null, null, null, null);

        ConflictException error = assertThrows(ConflictException.class, () -> service.crearUsuario(datos()));
        assertEquals(ConflictException.class, error.getClass());
        verify(repo, never()).save(any());
    }

    private UsuarioCrearDTO datos() {
        UsuarioCrearDTO datos = new UsuarioCrearDTO();
        datos.setEmail(" ANA@example.com ");
        datos.setPassword("Clave123!");
        return datos;
    }
}
