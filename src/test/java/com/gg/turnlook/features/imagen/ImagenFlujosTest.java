package com.gg.turnlook.features.imagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gg.turnlook.features.servicio.Servicio;
import com.gg.turnlook.features.sucursal.Sucursal;
import com.gg.turnlook.features.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ImagenFlujosTest {

    private ImagenRepository imagenRepo;
    private ImagenCloudinaryService cloudinaryService;
    private ImagenService imagenService;

    @BeforeEach
    void configurar() {
        imagenRepo = mock(ImagenRepository.class);
        cloudinaryService = mock(ImagenCloudinaryService.class);
        imagenService = new ImagenService(imagenRepo, cloudinaryService);
        when(imagenRepo.save(any(Imagen.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void guardaFotoNuevaEnPerfilSucursalYServicio() {
        Usuario usuario = new Usuario();
        Sucursal sucursal = new Sucursal();
        Servicio servicio = new Servicio();

        guardarComoFoto(usuario, sucursal, servicio, "nueva");

        assertEquals("appointmee/perfiles/nueva", usuario.getFotoPerfil().getPublicId());
        assertEquals("appointmee/sucursales/nueva", sucursal.getFotoPerfil().getPublicId());
        assertEquals("appointmee/servicios/nueva", servicio.getFotoPerfil().getPublicId());
        assertEquals(url("perfiles", "nueva"), usuario.getFotoPerfil().getUrl());
        assertEquals(url("sucursales", "nueva"), sucursal.getFotoPerfil().getUrl());
        assertEquals(url("servicios", "nueva"), servicio.getFotoPerfil().getUrl());
    }

    @Test
    void reemplazaLasTresFotosYEliminaLasAnteriores() {
        Usuario usuario = new Usuario();
        Sucursal sucursal = new Sucursal();
        Servicio servicio = new Servicio();
        usuario.setFotoPerfil(imagen("perfiles", "anterior"));
        sucursal.setFotoPerfil(imagen("sucursales", "anterior"));
        servicio.setFotoPerfil(imagen("servicios", "anterior"));
        Imagen imagenUsuario = usuario.getFotoPerfil();
        Imagen imagenSucursal = sucursal.getFotoPerfil();
        Imagen imagenServicio = servicio.getFotoPerfil();

        guardarComoFoto(usuario, sucursal, servicio, "nueva");

        assertSame(imagenUsuario, usuario.getFotoPerfil());
        assertSame(imagenSucursal, sucursal.getFotoPerfil());
        assertSame(imagenServicio, servicio.getFotoPerfil());
        assertEquals("appointmee/perfiles/nueva", imagenUsuario.getPublicId());
        assertEquals("appointmee/sucursales/nueva", imagenSucursal.getPublicId());
        assertEquals("appointmee/servicios/nueva", imagenServicio.getPublicId());
        verify(cloudinaryService).eliminar("appointmee/perfiles/anterior");
        verify(cloudinaryService).eliminar("appointmee/sucursales/anterior");
        verify(cloudinaryService).eliminar("appointmee/servicios/anterior");
    }

    @Test
    void conservarLaMismaUrlNoBorraLaFotoDeCloudinary() {
        Usuario usuario = new Usuario();
        usuario.setFotoPerfil(imagen("perfiles", "actual"));

        imagenService.cambiarFotoPerfilUsuario(usuario, url("perfiles", "actual"), null);

        assertEquals("appointmee/perfiles/actual", usuario.getFotoPerfil().getPublicId());
        verify(cloudinaryService, never()).eliminar(any());
    }

    @Test
    void reemplazarUnaFotoCompartidaNoBorraElArchivoQueOtroPerfilUsa() {
        Usuario usuario = new Usuario();
        usuario.setFotoPerfil(imagen("perfiles", "compartida"));
        when(cloudinaryService.esImagenPropia(any())).thenReturn(true);
        when(imagenRepo.contarEnUsoPorPublicId("appointmee/perfiles/compartida")).thenReturn(1L);

        imagenService.cambiarFotoPerfilUsuario(usuario, url("perfiles", "nueva"), "appointmee/perfiles/nueva");

        verify(cloudinaryService, never()).eliminar("appointmee/perfiles/compartida");
    }

    @Test
    void eliminarFotoDelPerfilBorraLaReferenciaYElArchivo() {
        Usuario usuario = new Usuario();
        usuario.setFotoPerfil(imagen("perfiles", "actual"));

        imagenService.eliminarFotoPerfilUsuario(usuario);

        assertEquals(null, usuario.getFotoPerfil().getUrl());
        assertEquals(null, usuario.getFotoPerfil().getPublicId());
        verify(cloudinaryService).eliminar("appointmee/perfiles/actual");
    }

    private void guardarComoFoto(Usuario usuario, Sucursal sucursal, Servicio servicio, String nombre) {
        when(cloudinaryService.esImagenPropia(any())).thenReturn(true);
        imagenService.cambiarFotoPerfilUsuario(usuario, url("perfiles", nombre), "appointmee/perfiles/" + nombre);
        imagenService.cambiarFotoPerfilSucursal(sucursal, url("sucursales", nombre), "appointmee/sucursales/" + nombre);
        imagenService.cambiarFotoPerfilServicio(servicio, url("servicios", nombre), "appointmee/servicios/" + nombre);
    }

    private Imagen imagen(String carpeta, String nombre) {
        Imagen imagen = new Imagen();
        imagen.setUrl(url(carpeta, nombre));
        imagen.setPublicId("appointmee/" + carpeta + "/" + nombre);
        return imagen;
    }

    private String url(String carpeta, String nombre) {
        return "https://res.cloudinary.com/cloud/image/upload/appointmee/" + carpeta + "/" + nombre + ".jpg";
    }
}
