package com.gg.turnlook.features.sucursal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gg.turnlook.common.exception.BadRequestException;
import com.gg.turnlook.common.exception.ConflictException;
import com.gg.turnlook.features.imagen.ImagenRepository;
import com.gg.turnlook.features.imagen.ImagenService;
import com.gg.turnlook.features.resenia.ReseniaService;
import com.gg.turnlook.features.solicitud.empleado.GestionEmpleadoService;
import com.gg.turnlook.features.sucursal.categoria.Categoria;
import com.gg.turnlook.features.sucursal.categoria.CategoriaRepository;
import com.gg.turnlook.features.sucursal.categoria.ECategoria;
import com.gg.turnlook.features.sucursal.dto.SucursalCrearDTO;
import com.gg.turnlook.features.sucursal.dto.SucursalMiniDTO;
import com.gg.turnlook.features.sucursal.dto.SucursalModificarDTO;
import com.gg.turnlook.features.sucursal.dto.SucursalResponseDTO;
import com.gg.turnlook.features.usuario.Usuario;
import com.gg.turnlook.features.usuario.UsuarioService;
import com.gg.turnlook.features.usuario.rol.RolService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

class SucursalUbicacionTest {

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
    private Validator validator;

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
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void creacionConUbicacionValidaPersisteLosTresValores() {
        prepararCreacion();

        service.crearSucursal(crearDTO(-34.6037, -58.3816, "ChIJ_valido"), EMPLEADOR);

        ArgumentCaptor<Sucursal> captor = ArgumentCaptor.forClass(Sucursal.class);
        verify(sucRepo).save(captor.capture());
        assertEquals(-34.6037, captor.getValue().getLatitud());
        assertEquals(-58.3816, captor.getValue().getLongitud());
        assertEquals("ChIJ_valido", captor.getValue().getGooglePlaceId());
    }

    @Test
    void rechazaCoordenadasFueraDeRangoYLosLimitesSonInclusivos() {
        assertTrue(camposConError(crearDTO(91, -58, "ChIJ_valido")).contains("latitud"));
        assertTrue(camposConError(crearDTO(-91, -58, "ChIJ_valido")).contains("latitud"));
        assertTrue(camposConError(crearDTO(-34, 181, "ChIJ_valido")).contains("longitud"));
        assertTrue(camposConError(crearDTO(-34, -181, "ChIJ_valido")).contains("longitud"));

        assertTrue(camposConError(crearDTO(-90, 180, "ChIJ_valido")).isEmpty());
    }

    @Test
    void rechazaUbicacionIncompletaEnCreacion() {
        SucursalCrearDTO sinLongitud = crearDTO(-34, -58, "ChIJ_valido");
        sinLongitud.setLongitud(null);
        assertTrue(camposConError(sinLongitud).contains("longitud"));

        SucursalCrearDTO sinPlaceId = crearDTO(-34, -58, "   ");
        assertTrue(camposConError(sinPlaceId).contains("googlePlaceId"));
    }

    @Test
    void direccionAceptaHasta255Caracteres() {
        SucursalCrearDTO limite = crearDTO(-34, -58, "ChIJ_valido");
        limite.setDireccion("A".repeat(255));
        assertTrue(camposConError(limite).isEmpty());

        SucursalCrearDTO excedida = crearDTO(-34, -58, "ChIJ_valido");
        excedida.setDireccion("A".repeat(256));
        assertTrue(camposConError(excedida).contains("direccion"));

        SucursalModificarDTO modificacion = new SucursalModificarDTO();
        modificacion.setDireccion("B".repeat(255));
        assertTrue(camposConError(modificacion).isEmpty());
    }

    @Test
    void modificacionActualizaLosTresValoresDeUbicacion() {
        Sucursal suc = sucursalBase(true);
        prepararModificacion(suc);

        SucursalModificarDTO dto = new SucursalModificarDTO();
        dto.setLatitud(-34.7);
        dto.setLongitud(-58.4);
        dto.setGooglePlaceId("ChIJ_nuevo");

        service.modificarSucursal(dto, ID, EMPLEADOR);

        assertEquals(-34.7, suc.getLatitud());
        assertEquals(-58.4, suc.getLongitud());
        assertEquals("ChIJ_nuevo", suc.getGooglePlaceId());
        verify(sucRepo).save(suc);
    }

    @Test
    void modificacionSinUbicacionConservaLosValoresExistentes() {
        Sucursal suc = sucursalBase(true);
        prepararModificacion(suc);

        SucursalModificarDTO dto = new SucursalModificarDTO();
        dto.setNombre("Sucursal modificada");

        service.modificarSucursal(dto, ID, EMPLEADOR);

        assertEquals(-34.1, suc.getLatitud());
        assertEquals(-58.2, suc.getLongitud());
        assertEquals("ChIJ_viejo", suc.getGooglePlaceId());
    }

    @Test
    void cambioDeDireccionConUbicacionPreviaExigeUbicacionCompleta() {
        Sucursal suc = sucursalBase(true);
        prepararModificacion(suc);

        SucursalModificarDTO dto = new SucursalModificarDTO();
        dto.setDireccion("Av. Nueva 2222, CABA");
        BadRequestException sinUbicacion = assertThrows(BadRequestException.class,
                () -> service.modificarSucursal(dto, ID, EMPLEADOR));
        assertTrue(sinUbicacion.getMessage().contains("ubicacion"));

        dto.setLatitud(-34.7);
        BadRequestException parcial = assertThrows(BadRequestException.class,
                () -> service.modificarSucursal(dto, ID, EMPLEADOR));
        assertTrue(parcial.getMessage().contains("ubicacion"));

        assertEquals("Calle Vieja 1111", suc.getDireccion());
        verify(sucRepo, never()).save(any());
    }

    @Test
    void cambioDeDireccionYUbicacionSeAplicanJuntos() {
        Sucursal suc = sucursalBase(true);
        prepararModificacion(suc);

        SucursalModificarDTO dto = new SucursalModificarDTO();
        dto.setDireccion("Av. Nueva 2222, CABA");
        dto.setLatitud(-34.7);
        dto.setLongitud(-58.4);
        dto.setGooglePlaceId("ChIJ_nuevo");

        service.modificarSucursal(dto, ID, EMPLEADOR);

        assertEquals("Av. Nueva 2222, CABA", suc.getDireccion());
        assertEquals(-34.7, suc.getLatitud());
        assertEquals(-58.4, suc.getLongitud());
        assertEquals("ChIJ_nuevo", suc.getGooglePlaceId());
    }

    @Test
    void cambioDeDireccionSinUbicacionPreviaNoExigeUbicacion() {
        Sucursal suc = sucursalBase(false);
        prepararModificacion(suc);

        SucursalModificarDTO dto = new SucursalModificarDTO();
        dto.setDireccion("Av. Nueva 2222, CABA");

        service.modificarSucursal(dto, ID, EMPLEADOR);

        assertEquals("Av. Nueva 2222, CABA", suc.getDireccion());
        assertNull(suc.getLatitud());
        assertNull(suc.getLongitud());
        assertNull(suc.getGooglePlaceId());
    }

    @Test
    void placeIdDuplicadoAlCrearSeRechazaAntesDeGuardar() {
        prepararCreacion();
        when(sucRepo.existsByActivoTrueAndGooglePlaceId(anyString())).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.crearSucursal(crearDTO(-34, -58, "ChIJ_duplicado"), EMPLEADOR));

        assertTrue(ex.getMessage().contains("Place ID"));
        verify(sucRepo, never()).save(any());
    }

    @Test
    void placeIdDuplicadoAlModificarSeRechazaAntesDeGuardar() {
        Sucursal suc = sucursalBase(true);
        prepararModificacion(suc);
        when(sucRepo.existsByActivoTrueAndGooglePlaceIdAndIdNot("ChIJ_de_otra", ID)).thenReturn(true);

        SucursalModificarDTO dto = new SucursalModificarDTO();
        dto.setLatitud(-34.7);
        dto.setLongitud(-58.4);
        dto.setGooglePlaceId("ChIJ_de_otra");

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.modificarSucursal(dto, ID, EMPLEADOR));

        assertTrue(ex.getMessage().contains("Place ID"));
        verify(sucRepo, never()).save(any());
    }

    @Test
    void violacionDelIndiceUnicoDePlaceIdSeTraduceAConflicto() {
        prepararCreacion();
        SQLException causa = new SQLException(
                "ERROR: duplicate key value violates unique constraint \"ux_sucursales_google_place_id\"");
        doThrow(new DataIntegrityViolationException("could not execute statement", causa))
                .when(sucRepo).save(any(Sucursal.class));

        assertThrows(ConflictException.class,
                () -> service.crearSucursal(crearDTO(-34, -58, "ChIJ_valido"), EMPLEADOR));
    }

    @Test
    void listadosIncluyenCoordenadasYPermitenValoresNulos() {
        Sucursal conUbicacion = sucursalBase(true);
        conUbicacion.setId(1);
        Sucursal sinUbicacion = sucursalBase(false);
        sinUbicacion.setId(2);
        sinUbicacion.setDireccion("Calle Sin Ubicacion 3333");

        when(sucRepo.findOperativas(null, null)).thenReturn(List.of(conUbicacion, sinUbicacion));
        when(reseniaService.getPuntuacionesSucursales(anyList())).thenReturn(Map.of());

        List<SucursalMiniDTO> listado = service.listarSucursales();

        verify(sucRepo).findOperativas(null, null);
        assertEquals(2, listado.size());
        SucursalMiniDTO con = listado.stream().filter(d -> d.getId().equals(1)).findFirst().orElseThrow();
        assertEquals(-34.1, con.getLatitud());
        assertEquals(-58.2, con.getLongitud());
        SucursalMiniDTO sin = listado.stream().filter(d -> d.getId().equals(2)).findFirst().orElseThrow();
        assertNull(sin.getLatitud());
        assertNull(sin.getLongitud());
    }

    @Test
    void detalleDevuelveUbicacionGuardada() {
        Sucursal suc = sucursalBase(true);
        prepararDetalle(suc);

        SucursalResponseDTO dto = service.verSucursalPorId(ID);

        assertEquals(-34.1, dto.getLatitud());
        assertEquals(-58.2, dto.getLongitud());
        assertEquals("ChIJ_viejo", dto.getGooglePlaceId());
    }

    @Test
    void detallePermiteUbicacionNula() {
        Sucursal suc = sucursalBase(false);
        prepararDetalle(suc);

        SucursalResponseDTO dto = service.verSucursalPorId(ID);

        assertNull(dto.getLatitud());
        assertNull(dto.getLongitud());
        assertNull(dto.getGooglePlaceId());
    }

    private void prepararCreacion() {
        when(catRepo.findById(1)).thenReturn(Optional.of(new Categoria(ECategoria.BARBERIA)));
        when(usuarioService.listarUsuarioPorEmail(EMPLEADOR)).thenReturn(mock(Usuario.class));
        when(sucRepo.existsByDireccionIgnoreCase(anyString())).thenReturn(false);
        when(sucRepo.existsByActivoTrueAndGooglePlaceId(anyString())).thenReturn(false);
    }

    private void prepararModificacion(Sucursal suc) {
        when(sucRepo.findByIdConEmpleador(ID)).thenReturn(Optional.of(suc));
        when(sucRepo.existsByActivoTrueAndGooglePlaceIdAndIdNot(anyString(), anyInt())).thenReturn(false);
    }

    private void prepararDetalle(Sucursal suc) {
        when(sucRepo.findByIdConDetalle(ID)).thenReturn(Optional.of(suc));
        when(imagenService.listarImagenesPorSucursal(suc)).thenReturn(List.of());
        when(reseniaService.getPuntuacionesSucursales(anyList())).thenReturn(Map.of());
    }

    private Set<String> camposConError(Object dto) {
        return validator.validate(dto).stream()
                .map(violacion -> violacion.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    private SucursalCrearDTO crearDTO(double latitud, double longitud, String googlePlaceId) {
        SucursalCrearDTO dto = new SucursalCrearDTO();
        dto.setNombre("Sucursal Centro");
        dto.setDireccion("Av. Libertador 1234, CABA");
        dto.setTelefono("12345678");
        dto.setDescripcion("Sucursal de prueba con descripcion valida");
        dto.setCategoriaId(1);
        dto.setHoraApertura(LocalTime.of(9, 0));
        dto.setHoraCierre(LocalTime.of(18, 0));
        dto.setDiasApertura(List.of("LUNES"));
        dto.setFotoUrl("https://example.com/foto.jpg");
        dto.setFotoPublicId("foto");
        dto.setLatitud(latitud);
        dto.setLongitud(longitud);
        dto.setGooglePlaceId(googlePlaceId);
        return dto;
    }

    private Sucursal sucursalBase(boolean conUbicacion) {
        Sucursal suc = new Sucursal();
        suc.setId(ID);
        suc.setNombre("Sucursal Centro");
        suc.setDireccion("Calle Vieja 1111");
        suc.setCategoria(new Categoria(ECategoria.BARBERIA));
        suc.setHoraApertura(LocalTime.of(9, 0));
        suc.setHoraCierre(LocalTime.of(18, 0));
        suc.setActivo(true);

        Usuario empleador = mock(Usuario.class);
        when(empleador.getEmail()).thenReturn(EMPLEADOR);
        suc.setEmpleador(empleador);

        if (conUbicacion) {
            suc.setLatitud(-34.1);
            suc.setLongitud(-58.2);
            suc.setGooglePlaceId("ChIJ_viejo");
        }
        return suc;
    }
}
