package com.gg.turnlook.features.sucursal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gg.turnlook.common.exception.ConflictException;
import com.gg.turnlook.features.imagen.ImagenRepository;
import com.gg.turnlook.features.imagen.ImagenService;
import com.gg.turnlook.features.resenia.ReseniaService;
import com.gg.turnlook.features.solicitud.empleado.GestionEmpleadoService;
import com.gg.turnlook.features.sucursal.categoria.Categoria;
import com.gg.turnlook.features.sucursal.categoria.CategoriaRepository;
import com.gg.turnlook.features.sucursal.categoria.ECategoria;
import com.gg.turnlook.features.sucursal.dto.SucursalCrearDTO;
import com.gg.turnlook.features.sucursal.dto.SucursalModificarDTO;
import com.gg.turnlook.features.usuario.Usuario;
import com.gg.turnlook.features.usuario.UsuarioService;
import com.gg.turnlook.features.usuario.rol.RolService;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SucursalPlaceIdLiberacionTest {

    private static final String EMPLEADOR = "empleador@test.com";
    private static final int ID = 7;

    private SucursalRepository sucRepo;
    private CategoriaRepository catRepo;
    private ImagenRepository imagenRepo;
    private UsuarioService usuarioService;
    private ImagenService imagenService;
    private ReseniaService reseniaService;
    private GestionEmpleadoService gestionEmpleadoService;
    private RolService rolService;
    private SucursalService service;

    @BeforeEach
    void setUp() {
        sucRepo = mock(SucursalRepository.class);
        catRepo = mock(CategoriaRepository.class);
        imagenRepo = mock(ImagenRepository.class);
        usuarioService = mock(UsuarioService.class);
        imagenService = mock(ImagenService.class);
        reseniaService = mock(ReseniaService.class);
        gestionEmpleadoService = mock(GestionEmpleadoService.class);
        rolService = mock(RolService.class);
        service = new SucursalService(sucRepo, catRepo, imagenRepo, usuarioService,
                imagenService, reseniaService, gestionEmpleadoService, rolService);
    }

    @Test
    void borrarSucursalPropiaLiberaPlaceIdYConservaLosDemasDatos() {
        Sucursal suc = sucursalBase();
        when(sucRepo.findByIdConEmpleador(ID)).thenReturn(Optional.of(suc));

        service.borrarSucursalPropia(ID, EMPLEADOR);

        assertFalse(suc.isActivo());
        assertNull(suc.getGooglePlaceId());
        assertEquals("Sucursal Centro", suc.getNombre());
        assertEquals("Calle Vieja 1111", suc.getDireccion());
        assertEquals(-34.1, suc.getLatitud());
        assertEquals(-58.2, suc.getLongitud());
        assertEquals(LocalTime.of(9, 0), suc.getHoraApertura());
        assertEquals(LocalTime.of(18, 0), suc.getHoraCierre());
        verify(sucRepo).save(suc);
    }

    @Test
    void eliminarSucursalComoAdministradorTambienLiberaPlaceId() {
        Sucursal suc = sucursalBase();
        when(sucRepo.findByIdConEmpleador(ID)).thenReturn(Optional.of(suc));

        service.eliminarSucursal(ID);

        assertFalse(suc.isActivo());
        assertNull(suc.getGooglePlaceId());
        assertEquals("Calle Vieja 1111", suc.getDireccion());
        verify(sucRepo).save(suc);
    }

    @Test
    void placeIdLiberadoPuedeAsignarseAOtraSucursalAlCrearla() {
        prepararCreacion();
        when(sucRepo.existsByActivoTrueAndGooglePlaceId("ChIJ_liberado")).thenReturn(false);

        service.crearSucursal(crearDTO("ChIJ_liberado"), EMPLEADOR);

        ArgumentCaptor<Sucursal> captor = ArgumentCaptor.forClass(Sucursal.class);
        verify(sucRepo).save(captor.capture());
        assertEquals("ChIJ_liberado", captor.getValue().getGooglePlaceId());
        assertTrue(captor.getValue().isActivo());
    }

    @Test
    void placeIdDeSucursalActivaNoPuedeAsignarseAlCrear() {
        prepararCreacion();
        when(sucRepo.existsByActivoTrueAndGooglePlaceId("ChIJ_ocupado")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.crearSucursal(crearDTO("ChIJ_ocupado"), EMPLEADOR));

        assertTrue(ex.getMessage().contains("Place ID"));
        verify(sucRepo, never()).save(any());
    }

    @Test
    void placeIdQueTeniaUnaSucursalInactivaNoBloqueaLaCreacion() {
        Sucursal inactiva = sucursalBase();
        inactiva.setActivo(false);
        // Simula una fila historica: la app ya no deja Place ID en inactivas,
        // pero la validacion tampoco debe consultarlas.
        inactiva.setGooglePlaceId("ChIJ_historico");
        when(sucRepo.findByIdConEmpleador(ID)).thenReturn(Optional.of(inactiva));
        service.eliminarSucursal(ID);
        assertNull(inactiva.getGooglePlaceId());

        prepararCreacion();
        when(sucRepo.existsByActivoTrueAndGooglePlaceId("ChIJ_historico")).thenReturn(false);

        service.crearSucursal(crearDTO("ChIJ_historico"), EMPLEADOR);

        ArgumentCaptor<Sucursal> captor = ArgumentCaptor.forClass(Sucursal.class);
        verify(sucRepo, org.mockito.Mockito.times(2)).save(captor.capture());
        assertEquals("ChIJ_historico", captor.getAllValues().get(1).getGooglePlaceId());
    }

    @Test
    void modificacionPuedeUsarPlaceIdLiberadoPorOtraSucursal() {
        Sucursal suc = sucursalBase();
        when(sucRepo.findByIdConEmpleador(ID)).thenReturn(Optional.of(suc));
        when(sucRepo.existsByActivoTrueAndGooglePlaceIdAndIdNot("ChIJ_liberado", ID))
                .thenReturn(false);

        SucursalModificarDTO dto = new SucursalModificarDTO();
        dto.setLatitud(-34.7);
        dto.setLongitud(-58.4);
        dto.setGooglePlaceId("ChIJ_liberado");

        service.modificarSucursal(dto, ID, EMPLEADOR);

        assertEquals("ChIJ_liberado", suc.getGooglePlaceId());
        verify(sucRepo).save(suc);
    }

    @Test
    void modificacionRechazaPlaceIdQuePerteneceAOtraSucursalActiva() {
        Sucursal suc = sucursalBase();
        when(sucRepo.findByIdConEmpleador(ID)).thenReturn(Optional.of(suc));
        when(sucRepo.existsByActivoTrueAndGooglePlaceIdAndIdNot("ChIJ_ocupado", ID))
                .thenReturn(true);

        SucursalModificarDTO dto = new SucursalModificarDTO();
        dto.setLatitud(-34.7);
        dto.setLongitud(-58.4);
        dto.setGooglePlaceId("ChIJ_ocupado");

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.modificarSucursal(dto, ID, EMPLEADOR));

        assertTrue(ex.getMessage().contains("Place ID"));
        assertEquals("ChIJ_viejo", suc.getGooglePlaceId());
        verify(sucRepo, never()).save(any());
    }

    private void prepararCreacion() {
        when(catRepo.findById(1)).thenReturn(Optional.of(new Categoria(ECategoria.BARBERIA)));
        when(usuarioService.listarUsuarioPorEmail(EMPLEADOR)).thenReturn(mock(Usuario.class));
        when(sucRepo.existsByDireccionIgnoreCase(anyString())).thenReturn(false);
    }

    private SucursalCrearDTO crearDTO(String googlePlaceId) {
        SucursalCrearDTO dto = new SucursalCrearDTO();
        dto.setNombre("Sucursal Nueva");
        dto.setDireccion("Av. Libertador 1234, CABA");
        dto.setTelefono("12345678");
        dto.setDescripcion("Sucursal de prueba con descripcion valida");
        dto.setCategoriaId(1);
        dto.setHoraApertura(LocalTime.of(9, 0));
        dto.setHoraCierre(LocalTime.of(18, 0));
        dto.setDiasApertura(List.of("LUNES"));
        dto.setFotoUrl("https://example.com/foto.jpg");
        dto.setFotoPublicId("foto");
        dto.setLatitud(-34.6037);
        dto.setLongitud(-58.3816);
        dto.setGooglePlaceId(googlePlaceId);
        return dto;
    }

    private Sucursal sucursalBase() {
        Sucursal suc = new Sucursal();
        suc.setId(ID);
        suc.setNombre("Sucursal Centro");
        suc.setDireccion("Calle Vieja 1111");
        suc.setDescripcion("Descripcion de la sucursal de prueba");
        suc.setTelefono("12345678");
        suc.setCategoria(new Categoria(ECategoria.BARBERIA));
        suc.setHoraApertura(LocalTime.of(9, 0));
        suc.setHoraCierre(LocalTime.of(18, 0));
        suc.setActivo(true);
        suc.setLatitud(-34.1);
        suc.setLongitud(-58.2);
        suc.setGooglePlaceId("ChIJ_viejo");

        Usuario empleador = mock(Usuario.class);
        when(empleador.getEmail()).thenReturn(EMPLEADOR);
        suc.setEmpleador(empleador);
        return suc;
    }
}
