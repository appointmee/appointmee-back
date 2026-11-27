package com.gg.turnlook.features.sucursal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gg.turnlook.common.exception.BadRequestException;
import com.gg.turnlook.common.exception.ConflictException;
import com.gg.turnlook.common.exception.ForbiddenException;
import com.gg.turnlook.common.exception.NotFoundException;
import com.gg.turnlook.features.imagen.ImagenRepository;
import com.gg.turnlook.features.imagen.ImagenService;
import com.gg.turnlook.features.resenia.ReseniaService;
import com.gg.turnlook.features.solicitud.empleado.GestionEmpleadoService;
import com.gg.turnlook.features.sucursal.categoria.Categoria;
import com.gg.turnlook.features.sucursal.categoria.CategoriaRepository;
import com.gg.turnlook.features.sucursal.categoria.ECategoria;
import com.gg.turnlook.features.sucursal.dto.SucursalReactivarDTO;
import com.gg.turnlook.features.usuario.Usuario;
import com.gg.turnlook.features.usuario.UsuarioService;
import com.gg.turnlook.features.usuario.rol.RolService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SucursalReactivacionTest {

    private static final String EMPLEADOR = "empleador@test.com";
    private static final String OTRO = "otro@test.com";
    private static final int ID = 7;

    private SucursalRepository sucRepo;
    private UsuarioService usuarioService;
    private SucursalService service;
    private Validator validator;

    @BeforeEach
    void setUp() {
        sucRepo = mock(SucursalRepository.class);
        CategoriaRepository catRepo = mock(CategoriaRepository.class);
        ImagenRepository imagenRepo = mock(ImagenRepository.class);
        usuarioService = mock(UsuarioService.class);
        ImagenService imagenService = mock(ImagenService.class);
        ReseniaService reseniaService = mock(ReseniaService.class);
        GestionEmpleadoService gestionEmpleadoService = mock(GestionEmpleadoService.class);
        RolService rolService = mock(RolService.class);
        service = new SucursalService(sucRepo, catRepo, imagenRepo, usuarioService,
                imagenService, reseniaService, gestionEmpleadoService, rolService);
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void sinGooglePlaceIdNoSePuedeReactivar() {
        prepararSucursalInactiva(sucursalInactiva(null));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.reactivarSucursal(ID, dto(null, null, null), EMPLEADOR));

        assertTrue(ex.getMessage().contains("Place ID"));
        verify(sucRepo, never()).save(any());
    }

    @Test
    void dtoRechazaPlaceIdVacio() {
        Set<String> campos = validator.validate(dto("   ", -34.1, -58.2)).stream()
                .map(violacion -> violacion.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertTrue(campos.contains("googlePlaceId"));
    }

    @Test
    void placeIdDeOtraSucursalActivaNoPermiteReactivar() {
        prepararSucursalInactiva(sucursalInactiva(null));
        when(sucRepo.existsByActivoTrueAndGooglePlaceIdAndIdNot("ChIJ_ocupado", ID))
                .thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.reactivarSucursal(ID, dto("ChIJ_ocupado", null, null), EMPLEADOR));

        assertTrue(ex.getMessage().contains("Place ID"));
        verify(sucRepo, never()).save(any());
    }

    @Test
    void reactivaConPlaceIdNuevoYDisponibleConservandoCoordenadas() {
        Sucursal suc = sucursalInactiva(null);
        prepararSucursalInactiva(suc);
        when(sucRepo.existsByActivoTrueAndGooglePlaceIdAndIdNot("ChIJ_nuevo", ID))
                .thenReturn(false);

        service.reactivarSucursal(ID, dto("ChIJ_nuevo", null, null), EMPLEADOR);

        assertTrue(suc.isActivo());
        assertEquals("ChIJ_nuevo", suc.getGooglePlaceId());
        assertEquals(-34.1, suc.getLatitud());
        assertEquals(-58.2, suc.getLongitud());
        assertEquals("Sucursal Centro", suc.getNombre());
        verify(sucRepo).save(suc);
    }

    @Test
    void reactivaConCoordenadasNuevasCuandoSeEnvian() {
        Sucursal suc = sucursalInactiva(null);
        prepararSucursalInactiva(suc);
        when(sucRepo.existsByActivoTrueAndGooglePlaceIdAndIdNot("ChIJ_nuevo", ID))
                .thenReturn(false);

        service.reactivarSucursal(ID, dto("ChIJ_nuevo", -34.9, -58.9), EMPLEADOR);

        assertTrue(suc.isActivo());
        assertEquals(-34.9, suc.getLatitud());
        assertEquals(-58.9, suc.getLongitud());
        verify(sucRepo).save(suc);
    }

    @Test
    void exigeUbicacionCompletaSiLaSucursalNoTeníaCoordenadas() {
        Sucursal suc = sucursalInactiva(null);
        suc.setLatitud(null);
        suc.setLongitud(null);
        prepararSucursalInactiva(suc);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.reactivarSucursal(ID, dto("ChIJ_nuevo", null, null), EMPLEADOR));

        assertTrue(ex.getMessage().contains("ubicacion"));
        assertFalse(suc.isActivo());
        verify(sucRepo, never()).save(any());
    }

    @Test
    void noPermiteReutilizarElMismoPlaceIdQueConservabaLaSucursalInactiva() {
        prepararSucursalInactiva(sucursalInactiva("ChIJ_viejo"));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.reactivarSucursal(ID, dto("ChIJ_viejo", null, null), EMPLEADOR));

        assertTrue(ex.getMessage().contains("nuevo"));
        verify(sucRepo, never()).save(any());
    }

    @Test
    void soloSePuedeReactivarUnaSucursalInactiva() {
        when(sucRepo.findByIdInactivaConEmpleador(ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.reactivarSucursal(ID, dto("ChIJ_nuevo", null, null), EMPLEADOR));

        verify(sucRepo, never()).save(any());
    }

    @Test
    void usuarioSinPermisosNoPuedeReactivar() {
        prepararSucursalInactiva(sucursalInactiva(null));
        Usuario otro = mock(Usuario.class);
        when(usuarioService.listarUsuarioPorEmail(OTRO)).thenReturn(otro);
        when(usuarioService.esAdmin(otro)).thenReturn(false);

        assertThrows(ForbiddenException.class,
                () -> service.reactivarSucursal(ID, dto("ChIJ_nuevo", null, null), OTRO));

        verify(sucRepo, never()).save(any());
    }

    private void prepararSucursalInactiva(Sucursal suc) {
        when(sucRepo.findByIdInactivaConEmpleador(ID)).thenReturn(Optional.of(suc));
    }

    private SucursalReactivarDTO dto(String googlePlaceId, Double latitud, Double longitud) {
        SucursalReactivarDTO dto = new SucursalReactivarDTO();
        dto.setGooglePlaceId(googlePlaceId);
        dto.setLatitud(latitud);
        dto.setLongitud(longitud);
        return dto;
    }

    private Sucursal sucursalInactiva(String googlePlaceId) {
        Sucursal suc = new Sucursal();
        suc.setId(ID);
        suc.setNombre("Sucursal Centro");
        suc.setDireccion("Calle Vieja 1111");
        suc.setDescripcion("Descripcion de la sucursal de prueba");
        suc.setCategoria(new Categoria(ECategoria.BARBERIA));
        suc.setHoraApertura(LocalTime.of(9, 0));
        suc.setHoraCierre(LocalTime.of(18, 0));
        suc.setActivo(false);
        suc.setLatitud(-34.1);
        suc.setLongitud(-58.2);
        suc.setGooglePlaceId(googlePlaceId);

        Usuario empleador = mock(Usuario.class);
        when(empleador.getEmail()).thenReturn(EMPLEADOR);
        suc.setEmpleador(empleador);
        return suc;
    }
}
