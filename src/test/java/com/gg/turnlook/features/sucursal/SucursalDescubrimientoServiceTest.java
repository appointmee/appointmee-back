package com.gg.turnlook.features.sucursal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gg.turnlook.common.exception.BadRequestException;
import com.gg.turnlook.features.sucursal.categoria.Categoria;
import com.gg.turnlook.features.sucursal.categoria.ECategoria;
import com.gg.turnlook.features.sucursal.dto.PaginaSucursalesDTO;
import com.gg.turnlook.features.sucursal.dto.RecomendacionesSucursalesDTO;
import com.gg.turnlook.features.sucursal.dto.SucursalMiniDTO;
import com.gg.turnlook.features.turno.EstadoTurno;
import com.gg.turnlook.features.turno.TurnoRepository;
import com.gg.turnlook.features.usuario.Usuario;
import com.gg.turnlook.features.usuario.UsuarioService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;

/** Prueba la orquestacion del Home y del listado paginado con las consultas
 * simuladas, para validar limites, orden de categorias y metadata. */
@SuppressWarnings("unchecked")
class SucursalDescubrimientoServiceTest {

    private static final String EMAIL = "cliente@test.com";

    private SucursalRepository sucRepo;
    private TurnoRepository turnoRepo;
    private SucursalVisitaRepository visitaRepo;
    private UsuarioService usuarioService;
    private SucursalService sucursalService;
    private SucursalDescubrimientoService service;

    @BeforeEach
    void setUp() {
        sucRepo = mock(SucursalRepository.class);
        turnoRepo = mock(TurnoRepository.class);
        visitaRepo = mock(SucursalVisitaRepository.class);
        usuarioService = mock(UsuarioService.class);
        sucursalService = mock(SucursalService.class);

        when(sucursalService.listarSucursales()).thenReturn(List.of());
        when(sucursalService.mapearSucursalesMiniEnOrdenRecibido(anyList()))
                .thenAnswer(inv -> ((List<Sucursal>) inv.getArgument(0)).stream()
                        .map(sucursal -> dto(sucursal.getId(), "SPA"))
                        .toList());
        when(turnoRepo.contarTurnosRecientesPorSucursal(
                anyCollection(), any(), any())).thenReturn(List.of());
        when(sucRepo.findOperativasNuevas(any(), any())).thenReturn(List.of());
        when(visitaRepo.findSucursalesRecientesOperativas(anyInt(), any()))
                .thenReturn(List.of());
        when(turnoRepo.puntuarCategoriasPorCliente(
                anyString(), anyCollection(), any(), any(), any())).thenReturn(List.of());
        when(turnoRepo.findSucursalesYaReservadasPorCliente(
                anyString(), anyCollection())).thenReturn(List.of());
        when(sucRepo.findByIdsConCategoriaYFoto(anyList())).thenReturn(List.of());

        service = new SucursalDescubrimientoService(
                sucRepo, turnoRepo, visitaRepo, usuarioService, sucursalService);
    }

    @Test
    void mejoresYTodasUsanUnaSolaVezElRankingExistenteYLimitanSuTop() {
        List<SucursalMiniDTO> rankeadas = java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(id -> dto(id, "SPA"))
                .toList();
        when(sucursalService.listarSucursales()).thenReturn(rankeadas);

        RecomendacionesSucursalesDTO resultado = service.obtenerRecomendaciones(null);

        assertEquals(rankeadas.subList(0, 10), resultado.getMejores());
        assertEquals(rankeadas.subList(0, 4), resultado.getTodas());
        assertEquals(List.of(), resultado.getVistasRecientes());
        assertEquals(List.of(), resultado.getParaVos());
        verify(sucursalService).listarSucursales();
        verify(visitaRepo, never()).findSucursalesRecientesOperativas(anyInt(), any());
    }

    @Test
    void principalAnonimoDeSpringTambienDejaVaciasLasSeccionesPersonales() {
        List<SucursalMiniDTO> rankeadas = List.of(dto(1, "SPA"), dto(2, "SPA"));
        when(sucursalService.listarSucursales()).thenReturn(rankeadas);

        RecomendacionesSucursalesDTO resultado =
                service.obtenerRecomendaciones("anonymousUser");

        assertEquals(rankeadas.subList(0, 2), resultado.getMejores());
        assertEquals(rankeadas, resultado.getTodas());
        assertEquals(List.of(), resultado.getVistasRecientes());
        assertEquals(List.of(), resultado.getParaVos());
        verify(usuarioService, never()).listarUsuarioPorEmail(anyString());
        verify(visitaRepo, never()).findSucursalesRecientesOperativas(anyInt(), any());
        verify(turnoRepo, never()).puntuarCategoriasPorCliente(
                anyString(), anyCollection(), any(), any(), any());
    }

    @Test
    void masDemandadasMantieneElOrdenDelConteoYUsaSoloEstadosValidos() {
        when(turnoRepo.contarTurnosRecientesPorSucursal(
                anyCollection(), any(), any())).thenReturn(List.of(
                new Object[]{1, 5L}, new Object[]{2, 3L}));
        when(sucRepo.findByIdsConCategoriaYFoto(List.of(1, 2)))
                .thenReturn(List.of(sucursal(2), sucursal(1)));

        RecomendacionesSucursalesDTO resultado = service.obtenerRecomendaciones(null);

        assertEquals(List.of(1, 2), ids(resultado.getMasDemandadas()));

        ArgumentCaptor<java.time.LocalDateTime> desde = ArgumentCaptor.forClass(
                java.time.LocalDateTime.class);
        ArgumentCaptor<java.util.Collection<EstadoTurno>> estados =
                ArgumentCaptor.forClass(java.util.Collection.class);
        verify(turnoRepo).contarTurnosRecientesPorSucursal(
                estados.capture(), desde.capture(), any());
        assertEquals(List.of(EstadoTurno.CONFIRMADO, EstadoTurno.REALIZADO),
                List.copyOf(estados.getValue()));
        LocalDateTime ahora = LocalDateTime.now();
        assertTrue(desde.getValue().isAfter(ahora.minusDays(31)));
        assertTrue(desde.getValue().isBefore(ahora.minusDays(29)));
    }

    @Test
    void vistasRecientesUsaElUsuarioAutenticadoYRespetaElOrdenDeVisita() {
        Usuario usuario = usuario();
        when(usuarioService.listarUsuarioPorEmail(EMAIL)).thenReturn(usuario);
        when(visitaRepo.findSucursalesRecientesOperativas(eq(usuario.getId()), any()))
                .thenReturn(List.of(sucursal(4), sucursal(9)));

        RecomendacionesSucursalesDTO resultado = service.obtenerRecomendaciones(EMAIL);

        assertEquals(List.of(4, 9), ids(resultado.getVistasRecientes()));
        ArgumentCaptor<org.springframework.data.domain.Pageable> pageable =
                ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(visitaRepo).findSucursalesRecientesOperativas(eq(usuario.getId()),
                pageable.capture());
        assertEquals(10, pageable.getValue().getPageSize());
    }

    @Test
    void registrarVisitaCreaORecuperaUnaSolaFilaPorUsuarioYSucursal() {
        Usuario usuario = usuario();
        Sucursal sucursal = sucursal(10);
        when(usuarioService.listarUsuarioPorEmail(EMAIL)).thenReturn(usuario);
        when(sucursalService.listarSucursalPorId(10)).thenReturn(sucursal);
        when(visitaRepo.findByUsuarioIdAndSucursalId(usuario.getId(), 10))
                .thenReturn(Optional.empty());

        service.registrarVisita(10, EMAIL);

        ArgumentCaptor<SucursalVisita> nueva = ArgumentCaptor.forClass(SucursalVisita.class);
        verify(visitaRepo).save(nueva.capture());
        assertEquals(usuario, nueva.getValue().getUsuario());
        assertEquals(sucursal, nueva.getValue().getSucursal());
        assertNotNull(nueva.getValue().getFechaVisita());

        LocalDateTime anterior = LocalDateTime.now().minusDays(3);
        SucursalVisita existente = new SucursalVisita(5, usuario, sucursal, anterior);
        when(visitaRepo.findByUsuarioIdAndSucursalId(usuario.getId(), 10))
                .thenReturn(Optional.of(existente));

        service.registrarVisita(10, EMAIL);

        ArgumentCaptor<SucursalVisita> actualizada = ArgumentCaptor.forClass(
                SucursalVisita.class);
        verify(visitaRepo, org.mockito.Mockito.times(2)).save(actualizada.capture());
        assertEquals(existente, actualizada.getAllValues().get(1));
        assertTrue(actualizada.getAllValues().get(1).getFechaVisita().isAfter(anterior));
    }

    @Test
    void paraVosPriorizaCategoriasPonderadasYExcluyeSucursalesYaReservadas() {
        when(usuarioService.listarUsuarioPorEmail(EMAIL)).thenReturn(usuario());
        when(turnoRepo.puntuarCategoriasPorCliente(
                eq(EMAIL), anyCollection(), any(), any(), any())).thenReturn(List.of(
                new Object[]{ECategoria.SPA, 2.0},
                new Object[]{ECategoria.BARBERIA, 1.7}));
        when(turnoRepo.findSucursalesYaReservadasPorCliente(eq(EMAIL), anyCollection()))
                .thenReturn(List.of(2));
        when(sucursalService.listarSucursales()).thenReturn(List.of(
                dto(1, "SPA"), dto(2, "SPA"), dto(3, "BARBERIA"),
                dto(4, "SPA"), dto(5, "PELUQUERIA")));

        RecomendacionesSucursalesDTO resultado = service.obtenerRecomendaciones(EMAIL);

        assertEquals(List.of(1, 4, 3), ids(resultado.getParaVos()));

        ArgumentCaptor<LocalDateTime> hace30 = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> hace90 = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> hace180 = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(turnoRepo).puntuarCategoriasPorCliente(
                eq(EMAIL), anyCollection(), hace30.capture(), hace90.capture(),
                hace180.capture());
        LocalDateTime ahora = LocalDateTime.now();
        assertTrue(hace30.getValue().isAfter(ahora.minusDays(31)));
        assertTrue(hace90.getValue().isAfter(ahora.minusDays(91)));
        assertTrue(hace180.getValue().isAfter(ahora.minusMonths(6).minusDays(1)));
    }

    @Test
    void paginadoDevuelveContenidoOrdenadoYMetadata() {
        when(sucRepo.findIdsOperativasOrdenadasPorRanking(
                eq("SPA"), eq("centro"), any()))
                .thenReturn(List.of(3, 1));
        when(sucRepo.contarOperativas(ECategoria.SPA, "centro")).thenReturn(14L);
        when(sucRepo.findByIdsConCategoriaYFoto(List.of(3, 1)))
                .thenReturn(List.of(sucursal(1), sucursal(3)));

        PaginaSucursalesDTO pagina = service.listarPaginado(
                "centro", "spa", "1", "2");

        assertEquals(List.of(3, 1), pagina.getContent().stream()
                .map(SucursalMiniDTO::getId).toList());
        assertEquals(14, pagina.getTotalElements());
        assertEquals(7, pagina.getTotalPages());
        assertEquals(1, pagina.getNumber());
        assertEquals(2, pagina.getSize());
        verify(sucRepo).findIdsOperativasOrdenadasPorRanking(
                "SPA", "centro", PageRequest.of(1, 2));
    }

    @Test
    void paginadoRechazaParametrosInvalidos() {
        assertThrows(BadRequestException.class,
                () -> service.listarPaginado(null, null, "-1", "12"));
        assertThrows(BadRequestException.class,
                () -> service.listarPaginado(null, null, "0", "0"));
        assertThrows(BadRequestException.class,
                () -> service.listarPaginado(null, null, "0", "51"));
        assertThrows(BadRequestException.class,
                () -> service.listarPaginado(null, "categoria-invalida", "0", "12"));
        assertThrows(BadRequestException.class,
                () -> service.listarPaginado(null, null, "abc", "12"));
        verify(sucRepo, never()).findIdsOperativasOrdenadasPorRanking(
                any(), any(), any());
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario("Ana", "Gomez", EMAIL, "hash");
        usuario.setId(7);
        return usuario;
    }

    private Sucursal sucursal(Integer id) {
        Sucursal sucursal = new Sucursal();
        sucursal.setId(id);
        sucursal.setCategoria(new Categoria(ECategoria.SPA));
        return sucursal;
    }

    private SucursalMiniDTO dto(Integer id, String categoria) {
        return new SucursalMiniDTO(id, "Sucursal " + id, categoria, null,
                null, 0L, List.of(), null, null);
    }

    private List<Integer> ids(List<SucursalMiniDTO> sucursales) {
        return sucursales.stream().map(SucursalMiniDTO::getId).toList();
    }

}
