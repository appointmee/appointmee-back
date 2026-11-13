package com.gg.turnlook.features.imagen;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gg.turnlook.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ImagenPendienteServiceTest {

    private ImagenRepository imagenRepository;
    private ImagenCloudinaryService cloudinaryService;
    private ImagenPendienteService service;

    @BeforeEach
    void preparar() {
        imagenRepository = mock(ImagenRepository.class);
        cloudinaryService = mock(ImagenCloudinaryService.class);
        when(cloudinaryService.esImagenPropia("appointmee/perfiles/foto")).thenReturn(true);
        service = new ImagenPendienteService(imagenRepository, cloudinaryService, "clave-de-prueba");
    }

    @Test
    void eliminaLaImagenPendienteDelMismoUsuario() {
        String token = service.crearToken("appointmee/perfiles/foto", "usuario@ejemplo.com");

        service.descartar("appointmee/perfiles/foto", token, "usuario@ejemplo.com");

        verify(cloudinaryService).eliminar("appointmee/perfiles/foto");
        verify(imagenRepository).eliminarHuerfanasPorPublicId("appointmee/perfiles/foto");
    }

    @Test
    void conservaLaImagenSiLaBaseYaLaUsa() {
        String token = service.crearToken("appointmee/perfiles/foto", "usuario@ejemplo.com");
        when(imagenRepository.contarEnUsoPorPublicId("appointmee/perfiles/foto")).thenReturn(1L);

        service.descartar("appointmee/perfiles/foto", token, "usuario@ejemplo.com");

        verify(cloudinaryService, never()).eliminar("appointmee/perfiles/foto");
    }

    @Test
    void otroUsuarioNoPuedeDescartarLaImagen() {
        String token = service.crearToken("appointmee/perfiles/foto", "usuario@ejemplo.com");

        assertThrows(BadRequestException.class,
                () -> service.descartar("appointmee/perfiles/foto", token, "otro@ejemplo.com"));
        verify(cloudinaryService, never()).eliminar("appointmee/perfiles/foto");
    }

    @Test
    void unTokenFalsoNoPuedeDescartarLaImagen() {
        assertThrows(BadRequestException.class,
                () -> service.descartar("appointmee/perfiles/foto", "123.falso", "usuario@ejemplo.com"));
        verify(cloudinaryService, never()).eliminar("appointmee/perfiles/foto");
    }
}
