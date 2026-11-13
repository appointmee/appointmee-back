package com.gg.turnlook.features.servicio;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gg.turnlook.common.exception.ForbiddenException;
import com.gg.turnlook.features.imagen.ImagenService;
import com.gg.turnlook.features.resenia.ReseniaService;
import com.gg.turnlook.features.sucursal.Sucursal;
import com.gg.turnlook.features.sucursal.SucursalService;
import com.gg.turnlook.features.usuario.Usuario;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServicioEliminacionPermisosTest {
    private final ServicioRepository repository = mock(ServicioRepository.class);
    private final SucursalService sucursalService = mock(SucursalService.class);
    private final Servicio servicio = mock(Servicio.class);
    private final Sucursal sucursal = mock(Sucursal.class);
    private final Usuario empleador = mock(Usuario.class);

    private ServicioService service;

    @BeforeEach
    void setUp() {
        service = new ServicioService(repository, sucursalService, mock(ImagenService.class), mock(ReseniaService.class));
        when(repository.findById(15)).thenReturn(Optional.of(servicio));
        when(servicio.getSucursal()).thenReturn(sucursal);
        when(sucursal.getEmpleador()).thenReturn(empleador);
        when(empleador.getEmail()).thenReturn("dueno@example.com");
    }

    @Test
    void administradorPuedeEliminarServicioDeOtraSucursal() {
        service.eliminarServicio(15, "admin@example.com", true);

        verify(servicio).setActivo(false);
        verify(repository).save(servicio);
    }

    @Test
    void empleadorPropietarioPuedeEliminarSuServicio() {
        service.eliminarServicio(15, "dueno@example.com", false);

        verify(servicio).setActivo(false);
        verify(repository).save(servicio);
    }

    @Test
    void empleadorAjenoNoPuedeEliminarServicio() {
        assertThrows(ForbiddenException.class,
                () -> service.eliminarServicio(15, "ajeno@example.com", false));

        verify(servicio, never()).setActivo(false);
        verify(repository, never()).save(servicio);
    }
}
