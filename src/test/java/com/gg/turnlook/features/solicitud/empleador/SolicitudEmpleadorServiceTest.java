package com.gg.turnlook.features.solicitud.empleador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gg.turnlook.common.exception.BadRequestException;
import com.gg.turnlook.common.exception.ConflictException;
import com.gg.turnlook.common.exception.ForbiddenException;
import com.gg.turnlook.features.solicitud.EstadoSolicitud;
import com.gg.turnlook.features.solicitud.empleador.dto.PaginaSolicitudesEmpleadorDTO;
import com.gg.turnlook.features.solicitud.empleador.dto.SolicitudEmpleadorCrearDTO;
import com.gg.turnlook.features.solicitud.empleador.dto.SolicitudEmpleadorMiniDTO;
import com.gg.turnlook.features.solicitud.empleador.dto.SolicitudEmpleadorResponseDTO;
import com.gg.turnlook.features.solicitud.empleador.dto.UbicacionSolicitudDTO;
import com.gg.turnlook.features.usuario.Usuario;
import com.gg.turnlook.features.usuario.UsuarioService;
import com.gg.turnlook.features.usuario.rol.ERol;
import com.gg.turnlook.features.usuario.rol.Rol;
import com.gg.turnlook.features.usuario.rol.RolService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class SolicitudEmpleadorServiceTest {

    private static final String EMAIL = "ana@example.com";

    private SolicitudEmpleadorRepository repo;
    private UsuarioService usuarioService;
    private RolService rolService;
    private SolicitudEmpleadorService service;
    private Validator validator;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        repo = mock(SolicitudEmpleadorRepository.class);
        usuarioService = mock(UsuarioService.class);
        rolService = mock(RolService.class);
        service = new SolicitudEmpleadorService(repo, usuarioService, rolService);
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        usuario = new Usuario("Ana", "Gomez", EMAIL, "hash");
        usuario.setId(11);
        when(usuarioService.listarUsuarioPorEmail(EMAIL)).thenReturn(usuario);
    }

    @Test
    void registraConUnaUbicacionYGuardaLosCuatroDatos() {
        service.registrarSolicitud(dto("Quiero abrir una sucursal en el centro.",
                ubicacion("Av. Libertador 1234, CABA", -34.6037, -58.3816, "ChIJ_uno")), EMAIL);

        SolicitudEmpleador guardada = capturarGuardada();
        assertEquals(1, guardada.getUbicaciones().size());
        UbicacionSolicitud ubicacion = guardada.getUbicaciones().getFirst();
        assertEquals("Av. Libertador 1234, CABA", ubicacion.getDireccion());
        assertEquals(-34.6037, ubicacion.getLatitud());
        assertEquals(-58.3816, ubicacion.getLongitud());
        assertEquals("ChIJ_uno", ubicacion.getGooglePlaceId());
        assertEquals(guardada, ubicacion.getSolicitud());
        assertEquals(usuario, guardada.getUsuario());
    }

    @Test
    void registraVariasUbicacionesYConservaTodas() {
        service.registrarSolicitud(dto("Quiero abrir varias sucursales en la ciudad.",
                ubicacion("Direccion uno, CABA", -34.1, -58.1, "ChIJ_uno"),
                ubicacion("Direccion dos, CABA", -34.2, -58.2, "ChIJ_dos"),
                ubicacion("Direccion tres, CABA", -34.3, -58.3, "ChIJ_tres")), EMAIL);

        SolicitudEmpleador guardada = capturarGuardada();
        assertEquals(List.of("ChIJ_uno", "ChIJ_dos", "ChIJ_tres"),
                guardada.getUbicaciones().stream()
                        .map(UbicacionSolicitud::getGooglePlaceId).toList());
        assertEquals(List.of(-34.1, -34.2, -34.3),
                guardada.getUbicaciones().stream()
                        .map(UbicacionSolicitud::getLatitud).toList());
    }

    @Test
    void rechazaSolicitudSinUbicaciones() {
        BadRequestException error = assertThrows(BadRequestException.class,
                () -> service.registrarSolicitud(
                        dto("Quiero abrir una sucursal en el centro."), EMAIL));

        assertTrue(error.getMessage().contains("ubicacion"));
        verify(repo, never()).save(any());
    }

    @Test
    void rechazaUbicacionesIncompletasFueraDeRangoOMuyLargas() {
        List<SolicitudEmpleadorCrearDTO> invalidas = List.of(
                dto(motivoValido(), ubicacion(null, -34.0, -58.0, "ChIJ_uno")),
                dto(motivoValido(), ubicacion("   ", -34.0, -58.0, "ChIJ_uno")),
                dto(motivoValido(), ubicacion("Direccion valida, CABA", null, -58.0, "ChIJ_uno")),
                dto(motivoValido(), ubicacion("Direccion valida, CABA", 90.1, -58.0, "ChIJ_uno")),
                dto(motivoValido(), ubicacion("Direccion valida, CABA", -34.0, -180.1, "ChIJ_uno")),
                dto(motivoValido(), ubicacion("Direccion valida, CABA", -34.0, -58.0, "  ")),
                dto(motivoValido(), ubicacion("Direccion valida, CABA", -34.0, -58.0, "C".repeat(256))),
                dto(motivoValido(), ubicacion("D".repeat(501), -34.0, -58.0, "ChIJ_uno")));

        for (SolicitudEmpleadorCrearDTO invalida : invalidas) {
            assertThrows(BadRequestException.class,
                    () -> service.registrarSolicitud(invalida, EMAIL));
        }
        verify(repo, never()).save(any());
    }

    @Test
    void aceptaLimitesInclusivosDeCoordenadas() {
        service.registrarSolicitud(dto(motivoValido(),
                ubicacion("Direccion valida, CABA", -90, -180, "ChIJ_uno"),
                ubicacion("Otra direccion valida", 90, 180, "ChIJ_dos")), EMAIL);

        assertEquals(2, capturarGuardada().getUbicaciones().size());
    }

    @Test
    void aceptaComentarioDe3000CaracteresYRechaza3001() {
        SolicitudEmpleadorCrearDTO limite = dto("A".repeat(3000),
                ubicacion("Direccion valida, CABA", -34.0, -58.0, "ChIJ_uno"));
        assertTrue(validator.validate(limite).isEmpty());
        service.registrarSolicitud(limite, EMAIL);
        assertEquals(3000, capturarGuardada().getMotivo().length());

        SolicitudEmpleadorCrearDTO excedido = dto("A".repeat(3001),
                ubicacion("Direccion valida, CABA", -34.0, -58.0, "ChIJ_uno"));
        assertTrue(camposConError(excedido).contains("motivo"));
        assertThrows(BadRequestException.class,
                () -> service.registrarSolicitud(excedido, EMAIL));
    }

    @Test
    void dtoValidaListaVaciaYCadaUbicacion() {
        assertTrue(camposConError(dto(motivoValido())).contains("ubicaciones"));

        Set<String> campos = camposConError(dto(motivoValido(),
                ubicacion("Direccion valida, CABA", 91, -58, "ChIJ_uno"),
                ubicacion("Direccion valida, CABA", -34, -181, "ChIJ_dos")));
        assertTrue(campos.stream().anyMatch(c -> c.contains("latitud")));
        assertTrue(campos.stream().anyMatch(c -> c.contains("longitud")));
    }

    @Test
    void primeraPaginaSinFiltroIncluyeMetadatosYEspecificaPageRequest() {
        List<SolicitudEmpleadorMiniDTO> contenido = List.of(
                mini(3, EstadoSolicitud.APROBADA), mini(2, EstadoSolicitud.PENDIENTE),
                mini(1, EstadoSolicitud.RECHAZADA));
        when(repo.findMiniAdmin(isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(contenido, PageRequest.of(0, 10), 23));

        PaginaSolicitudesEmpleadorDTO pagina =
                service.listarSolicitudesAdministrativas(null, "0", "10");

        assertEquals(contenido, pagina.getContent());
        assertEquals(23, pagina.getTotalElements());
        assertEquals(3, pagina.getTotalPages());
        assertEquals(0, pagina.getNumber());
        assertEquals(10, pagina.getSize());
        assertEquals(List.of(EstadoSolicitud.APROBADA, EstadoSolicitud.PENDIENTE,
                        EstadoSolicitud.RECHAZADA),
                pagina.getContent().stream().map(SolicitudEmpleadorMiniDTO::getEstado).toList());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repo).findMiniAdmin(isNull(), captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(10, captor.getValue().getPageSize());
    }

    @Test
    void paginaIntermediaConFiltroCombinaEstadoYPaginacion() {
        when(repo.findMiniAdmin(eq(EstadoSolicitud.APROBADA), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(mini(5, EstadoSolicitud.APROBADA),
                                mini(4, EstadoSolicitud.APROBADA)),
                        PageRequest.of(1, 2), 5));

        PaginaSolicitudesEmpleadorDTO pagina =
                service.listarSolicitudesAdministrativas("aprobada", "1", "2");

        assertEquals(2, pagina.getContent().size());
        assertEquals(5, pagina.getTotalElements());
        assertEquals(3, pagina.getTotalPages());
        assertEquals(1, pagina.getNumber());
        assertEquals(2, pagina.getSize());
        assertTrue(pagina.getContent().stream()
                .allMatch(d -> d.getEstado() == EstadoSolicitud.APROBADA));

        ArgumentCaptor<EstadoSolicitud> estadoCaptor =
                ArgumentCaptor.forClass(EstadoSolicitud.class);
        ArgumentCaptor<Pageable> paginaCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repo).findMiniAdmin(estadoCaptor.capture(), paginaCaptor.capture());
        assertEquals(EstadoSolicitud.APROBADA, estadoCaptor.getValue());
        assertEquals(PageRequest.of(1, 2), paginaCaptor.getValue());
    }

    @Test
    void paginaSinResultadosDevuelveMetadatosCorrectos() {
        when(repo.findMiniAdmin(eq(EstadoSolicitud.RECHAZADA), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(4, 10), 12));

        PaginaSolicitudesEmpleadorDTO pagina =
                service.listarSolicitudesAdministrativas("RECHAZADA", "4", "10");

        assertTrue(pagina.getContent().isEmpty());
        assertEquals(12, pagina.getTotalElements());
        assertEquals(2, pagina.getTotalPages());
        assertEquals(4, pagina.getNumber());
        assertEquals(10, pagina.getSize());
    }

    @Test
    void parametrosDePaginacionPorDefectoCuandoSeOmiten() {
        when(repo.findMiniAdmin(isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        PaginaSolicitudesEmpleadorDTO pagina =
                service.listarSolicitudesAdministrativas(null, null, null);

        assertEquals(0, pagina.getNumber());
        assertEquals(10, pagina.getSize());
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repo).findMiniAdmin(isNull(), captor.capture());
        assertEquals(PageRequest.of(0, 10), captor.getValue());
    }

    @Test
    void rechazaParametrosInvalidosYEstadoDesconocido() {
        List<String[]> casosInvalidos = List.of(
                new String[]{"RESUELTA", "0", "10"},
                new String[]{null, "-1", "10"},
                new String[]{null, "abc", "10"},
                new String[]{null, "0", "0"},
                new String[]{null, "0", "-5"},
                new String[]{null, "0", "abc"});

        for (String[] caso : casosInvalidos) {
            assertThrows(BadRequestException.class,
                    () -> service.listarSolicitudesAdministrativas(caso[0], caso[1], caso[2]));
        }
        verify(repo, never()).findMiniAdmin(any(), any());
    }

    @Test
    void detalleDeAdministradorIncluyeTodasLasUbicacionesYElSolicitante() {
        Usuario admin = new Usuario("Admin", "Sistemas", "admin@example.com", "hash");
        admin.setId(99);
        when(usuarioService.listarUsuarioPorEmail("admin@example.com")).thenReturn(admin);
        when(usuarioService.esAdmin(admin)).thenReturn(true);

        SolicitudEmpleador solicitud = solicitudDePrueba(EstadoSolicitud.PENDIENTE);
        solicitud.setId(7);
        solicitud.agregarUbicacion(ubicacionGuardada(1, "Av. Uno 1234", -34.1, -58.1, "ChIJ_uno"));
        solicitud.agregarUbicacion(ubicacionGuardada(2, "Av. Dos 2222", -34.2, -58.2, "ChIJ_dos"));
        when(repo.findByIdConUbicaciones(7)).thenReturn(Optional.of(solicitud));

        SolicitudEmpleadorResponseDTO detalle =
                service.verDetallesSolicitud(7, "admin@example.com");

        assertEquals(2, detalle.getUbicaciones().size());
        assertEquals("ChIJ_dos", detalle.getUbicaciones().get(1).getGooglePlaceId());
        assertTrue(detalle.getUbicaciones().getFirst().getUrlGoogleMaps()
                .contains("query_place_id=ChIJ_uno"));
        assertEquals(usuario.getId(), detalle.getUsuario().getId());
    }

    @Test
    void detalleHistoricoSinUbicacionesDevuelveColeccionVacia() {
        Usuario admin = new Usuario("Admin", "Sistemas", "admin@example.com", "hash");
        when(usuarioService.listarUsuarioPorEmail("admin@example.com")).thenReturn(admin);
        when(usuarioService.esAdmin(admin)).thenReturn(true);

        SolicitudEmpleador historica = solicitudDePrueba(EstadoSolicitud.RECHAZADA);
        when(repo.findByIdConUbicaciones(8)).thenReturn(Optional.of(historica));

        SolicitudEmpleadorResponseDTO detalle =
                service.verDetallesSolicitud(8, "admin@example.com");

        assertTrue(detalle.getUbicaciones().isEmpty());
        assertEquals(EstadoSolicitud.RECHAZADA, detalle.getEstado());
    }

    @Test
    void detalleDeOtroUsuarioEstaProhibido() {
        Usuario otro = new Usuario("Otro", "Usuario", "otro@example.com", "hash");
        otro.setId(50);
        when(usuarioService.listarUsuarioPorEmail("otro@example.com")).thenReturn(otro);

        SolicitudEmpleador solicitud = solicitudDePrueba(EstadoSolicitud.PENDIENTE);
        when(repo.findByIdConUbicaciones(9)).thenReturn(Optional.of(solicitud));

        assertThrows(ForbiddenException.class,
                () -> service.verDetallesSolicitud(9, "otro@example.com"));
    }

    @Test
    void apruebaAsignandoRolYRechazaSoloSiEstaPendiente() {
        SolicitudEmpleador pendiente = solicitudDePrueba(EstadoSolicitud.PENDIENTE);
        Rol empleador = mock(Rol.class);
        when(repo.findById(10)).thenReturn(Optional.of(pendiente));
        when(rolService.listarPorRol(ERol.EMPLEADOR)).thenReturn(empleador);

        service.aprobarSolicitud(10);

        assertEquals(EstadoSolicitud.APROBADA, pendiente.getEstado());
        verify(usuarioService).agregarRol(usuario, empleador);
        assertThrows(ConflictException.class, () -> service.rechazarSolicitud(10));
    }

    @Test
    void rechazaSolicitudPendiente() {
        SolicitudEmpleador pendiente = solicitudDePrueba(EstadoSolicitud.PENDIENTE);
        when(repo.findById(12)).thenReturn(Optional.of(pendiente));

        service.rechazarSolicitud(12);

        assertEquals(EstadoSolicitud.RECHAZADA, pendiente.getEstado());
        assertThrows(ConflictException.class, () -> service.aprobarSolicitud(12));
    }

    @Test
    void impideSolicitudPendienteDuplicada() {
        when(repo.existsByUsuarioIdAndEstado(usuario.getId(), EstadoSolicitud.PENDIENTE))
                .thenReturn(true);
        clearInvocations(repo);
        when(repo.existsByUsuarioIdAndEstado(usuario.getId(), EstadoSolicitud.PENDIENTE))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> service.registrarSolicitud(
                dto(motivoValido(), ubicacion("Direccion valida", -34, -58, "ChIJ_uno")), EMAIL));
        verify(repo, never()).save(any());
    }

    private SolicitudEmpleador capturarGuardada() {
        ArgumentCaptor<SolicitudEmpleador> captor = ArgumentCaptor.forClass(SolicitudEmpleador.class);
        verify(repo).save(captor.capture());
        return captor.getValue();
    }

    private SolicitudEmpleador solicitudDePrueba(EstadoSolicitud estado) {
        SolicitudEmpleador solicitud = new SolicitudEmpleador(motivoValido(), usuario);
        solicitud.setEstado(estado);
        solicitud.setFechaSolicitud(LocalDate.of(2026, 11, 20));
        solicitud.setUbicaciones(new ArrayList<>());
        return solicitud;
    }

    private SolicitudEmpleadorCrearDTO dto(String motivo, UbicacionSolicitudDTO... ubicaciones) {
        SolicitudEmpleadorCrearDTO dto = new SolicitudEmpleadorCrearDTO();
        dto.setMotivo(motivo);
        dto.setUbicaciones(new ArrayList<>(List.of(ubicaciones)));
        return dto;
    }

    private UbicacionSolicitudDTO ubicacion(String direccion, double latitud,
                                            double longitud, String placeId) {
        return ubicacion(direccion, Double.valueOf(latitud), Double.valueOf(longitud), placeId);
    }

    private UbicacionSolicitudDTO ubicacion(String direccion, Double latitud,
                                            Double longitud, String placeId) {
        UbicacionSolicitudDTO dto = new UbicacionSolicitudDTO();
        dto.setDireccion(direccion);
        dto.setLatitud(latitud);
        dto.setLongitud(longitud);
        dto.setGooglePlaceId(placeId);
        return dto;
    }

    private UbicacionSolicitud ubicacionGuardada(Integer id, String direccion,
                                                 double latitud, double longitud,
                                                 String placeId) {
        UbicacionSolicitud ubicacion = new UbicacionSolicitud(
                direccion, latitud, longitud, placeId);
        ubicacion.setId(id);
        return ubicacion;
    }

    private SolicitudEmpleadorMiniDTO mini(int id, EstadoSolicitud estado) {
        return new SolicitudEmpleadorMiniDTO(id, "Ana", "Gomez",
                LocalDate.of(2026, 11, 20), estado);
    }

    private String motivoValido() {
        return "Quiero abrir una sucursal en el centro de la ciudad.";
    }

    private Set<String> camposConError(Object dto) {
        return validator.validate(dto).stream()
                .map(violacion -> violacion.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

}
