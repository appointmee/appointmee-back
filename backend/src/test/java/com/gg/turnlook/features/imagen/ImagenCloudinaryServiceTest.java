package com.gg.turnlook.features.imagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.gg.turnlook.common.exception.BadRequestException;
import com.gg.turnlook.features.imagen.dto.ImagenSubidaDTO;
import java.util.Arrays;
import java.util.Map;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ImagenCloudinaryServiceTest {

    private Cloudinary cloudinary;
    private Uploader uploader;
    private ImagenCloudinaryService service;

    @BeforeEach
    void configurar() {
        cloudinary = mock(Cloudinary.class);
        uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        service = new ImagenCloudinaryService(cloudinary, "cloud", "key", "secret");
    }

    @Test
    void subeImagenValidaYDevuelveUrlSeguraYPublicId() throws Exception {
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of(
                "secure_url", "https://res.cloudinary.com/cloud/image/upload/appointmee/perfiles/foto.jpg",
                "public_id", "appointmee/perfiles/foto"
        ));
        MockMultipartFile archivo = new MockMultipartFile(
                "file", "foto.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
        );

        ImagenSubidaDTO subida = service.subir(archivo, ImagenCloudinaryTipo.PERFIL);

        assertEquals("https://res.cloudinary.com/cloud/image/upload/appointmee/perfiles/foto.jpg", subida.url());
        assertEquals("appointmee/perfiles/foto", subida.publicId());
        assertEquals("foto.jpg", subida.nombreArchivo());
        ArgumentCaptor<Map> opciones = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(any(), opciones.capture());
        assertEquals("appointmee/perfiles", opciones.getValue().get("folder"));
        assertEquals("appointmee/perfiles", opciones.getValue().get("asset_folder"));
        assertEquals(true, opciones.getValue().get("use_asset_folder_as_public_id_prefix"));
        assertEquals(5L * 1024 * 1024, opciones.getValue().get("max_file_size"));
        assertEquals(Arrays.asList("jpg", "jpeg", "png", "webp", "gif"),
                opciones.getValue().get("allowed_formats"));
    }

    @Test
    void rechazaArchivosQueNoSeanImagenAunqueElMimeDigaQueSi() {
        MockMultipartFile archivo = new MockMultipartFile(
                "file", "foto.jpg", "image/jpeg", "no es una imagen".getBytes()
        );

        assertThrows(BadRequestException.class,
                () -> service.subir(archivo, ImagenCloudinaryTipo.PERFIL));
        verify(cloudinary, never()).uploader();
    }

    @Test
    void rechazaTipoDeImagenNulo() {
        MockMultipartFile archivo = new MockMultipartFile(
                "file", "foto.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
        );

        assertThrows(BadRequestException.class, () -> service.subir(archivo, null));
        verify(cloudinary, never()).uploader();
    }

    @Test
    void rechazaImagenesQueSuperanLosCincoMegabytes() {
        byte[] contenido = new byte[5 * 1024 * 1024 + 1];
        contenido[0] = (byte) 0xFF;
        contenido[1] = (byte) 0xD8;
        contenido[2] = (byte) 0xFF;
        MockMultipartFile archivo = new MockMultipartFile("file", "foto.jpg", "image/jpeg", contenido);

        assertThrows(BadRequestException.class,
                () -> service.subir(archivo, ImagenCloudinaryTipo.PERFIL));
        verify(cloudinary, never()).uploader();
    }

    @Test
    void rechazaRespuestaIncompletaDeCloudinary() throws Exception {
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of("secure_url", "https://example.com/foto.jpg"));
        MockMultipartFile archivo = new MockMultipartFile(
                "file", "foto.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
        );

        assertThrows(BadRequestException.class,
                () -> service.subir(archivo, ImagenCloudinaryTipo.PERFIL));
    }

    @Test
    void rechazaSubidaSiFaltanCredenciales() {
        service = new ImagenCloudinaryService(cloudinary, "", "key", "secret");
        MockMultipartFile archivo = new MockMultipartFile(
                "file", "foto.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
        );

        assertThrows(BadRequestException.class,
                () -> service.subir(archivo, ImagenCloudinaryTipo.PERFIL));
        verify(cloudinary, never()).uploader();
    }

    @Test
    void soloEliminaPublicIdsDeLaCarpetaDeLaAplicacion() throws Exception {
        when(uploader.destroy(any(String.class), anyMap())).thenReturn(Map.of("result", "ok"));

        service.eliminar("appointmee/perfiles/foto");

        verify(uploader).destroy(eq("appointmee/perfiles/foto"), anyMap());
        assertThrows(BadRequestException.class, () -> service.eliminar("otra-aplicacion/foto"));
    }

    @Test
    void informaCuandoCloudinaryNoConfirmaLaEliminacion() throws Exception {
        when(uploader.destroy(any(String.class), anyMap())).thenReturn(Map.of("result", "error"));

        assertThrows(BadRequestException.class, () -> service.eliminar("appointmee/perfiles/foto"));
    }

    @Test
    void validaQueLaUrlSeaDelMismoCloudYPublicId() {
        service.validarReferencia("https://res.cloudinary.com/cloud/image/upload/v123/appointmee/perfiles/foto.png",
                "appointmee/perfiles/foto");
        assertThrows(BadRequestException.class, () -> service.validarReferencia(
                "https://res.cloudinary.com/otro/image/upload/appointmee/perfiles/foto.png", "appointmee/perfiles/foto"));
        assertThrows(BadRequestException.class, () -> service.validarReferencia(
                "https://res.cloudinary.com/cloud/image/upload/appointmee/perfiles/otra.png", "appointmee/perfiles/foto"));
        assertThrows(BadRequestException.class, () -> service.validarReferencia(
                "https://example.com/appointmee/perfiles/foto.png", "appointmee/perfiles/foto"));
    }

    @Test
    void invalidaLaCopiaEnCacheAlBorrar() throws Exception {
        when(uploader.destroy(any(String.class), anyMap())).thenReturn(Map.of("result", "ok"));
        service.eliminar("appointmee/perfiles/foto");
        verify(uploader).destroy(eq("appointmee/perfiles/foto"),
                org.mockito.ArgumentMatchers.argThat(opciones -> Boolean.TRUE.equals(opciones.get("invalidate"))));
    }

    @Test
    void descartaLaSubidaSiCloudinaryDevuelveElIdPeroNoUnaUrlValida() throws Exception {
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of("public_id", "appointmee/perfiles/foto"));
        when(uploader.destroy(anyString(), anyMap())).thenReturn(Map.of("result", "ok"));
        MockMultipartFile archivo = new MockMultipartFile("file", "foto.jpg", "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

        assertThrows(BadRequestException.class, () -> service.subir(archivo, ImagenCloudinaryTipo.PERFIL));
        verify(uploader).destroy(eq("appointmee/perfiles/foto"), anyMap());
    }
}
