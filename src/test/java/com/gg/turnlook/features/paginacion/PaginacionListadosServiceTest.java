package com.gg.turnlook.features.paginacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gg.turnlook.auth.BloqueoUsuario;
import com.gg.turnlook.auth.password.PasswordResetTokenRepository;
import com.gg.turnlook.auth.refresh.RefreshTokenRepository;
import com.gg.turnlook.common.dto.PaginaDTO;
import com.gg.turnlook.common.dto.VistaPreviaDTO;
import com.gg.turnlook.common.exception.BadRequestException;
import com.gg.turnlook.common.exception.ForbiddenException;
import com.gg.turnlook.features.imagen.ImagenService;
import com.gg.turnlook.features.notificacion.NotificacionEventosService;
import com.gg.turnlook.features.resenia.ReseniaRepository;
import com.gg.turnlook.features.resenia.ReseniaService;
import com.gg.turnlook.features.resenia.dto.ReseniaResponseDTO;
import com.gg.turnlook.features.servicio.ServicioService;
import com.gg.turnlook.features.solicitud.EstadoSolicitud;
import com.gg.turnlook.features.solicitud.empleado.SolicitudEmpleadoRepository;
import com.gg.turnlook.features.solicitud.empleado.SolicitudEmpleadoService;
import com.gg.turnlook.features.solicitud.empleado.dto.SolicitudEmpleadoMiniPropiaDTO;
import com.gg.turnlook.features.solicitud.empleado.dto.SolicitudEmpleadoMiniSucursalDTO;
import com.gg.turnlook.features.solicitud.empleador.SolicitudEmpleadorRepository;
import com.gg.turnlook.features.solicitud.empleador.SolicitudEmpleadorService;
import com.gg.turnlook.features.solicitud.empleador.dto.SolicitudEmpleadorMiniDTO;
import com.gg.turnlook.features.sucursal.Sucursal;
import com.gg.turnlook.features.sucursal.SucursalDescubrimientoService;
import com.gg.turnlook.features.sucursal.SucursalRepository;
import com.gg.turnlook.features.sucursal.SucursalService;
import com.gg.turnlook.features.sucursal.SucursalVisitaRepository;
import com.gg.turnlook.features.turno.EstadoTurno;
import com.gg.turnlook.features.turno.TurnoRepository;
import com.gg.turnlook.features.turno.TurnoService;
import com.gg.turnlook.features.turno.dto.TurnoMiniDTO;
import com.gg.turnlook.features.usuario.UsuarioRepository;
import com.gg.turnlook.features.usuario.UsuarioService;
import com.gg.turnlook.features.usuario.rol.RolService;
import com.gg.turnlook.features.verificacion.VerificacionRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/** Contrato de paginacion de los listados de SCRUM-367 en la capa de servicio. */
class PaginacionListadosServiceTest {

    private static final String EMAIL = "ana@example.com";

    @Test
    void reseniasDelDetalleTraeCincoYElHistorialUsaPaginasDeDiez() {
        ReseniaRepository repo = mock(ReseniaRepository.class);
        ReseniaService service = new ReseniaService(repo);
        List<ReseniaResponseDTO> resenias = resenias(5);

        when(repo.findPageReseniasPorSucursal(1, PageRequest.of(0, 5)))
                .thenReturn(new PageImpl<>(resenias, PageRequest.of(0, 5), 22));
        when(repo.findPageReseniasPorSucursal(1, PageRequest.of(2, 10)))
                .thenReturn(new PageImpl<>(resenias(2), PageRequest.of(2, 10), 22));

        VistaPreviaDTO<ReseniaResponseDTO> vistaPrevia = service.getReseniasPorSucursal(1);
        assertEquals(5, vistaPrevia.getContent().size());
        assertEquals(22, vistaPrevia.getTotalElements());
        assertTrue(vistaPrevia.isHayMas());

        PaginaDTO<ReseniaResponseDTO> pagina = service.listarReseniasPaginadas(1, "2");
        assertEquals(22, pagina.getTotalElements());
        assertEquals(3, pagina.getTotalPages());
        assertEquals(2, pagina.getNumber());
        assertEquals(10, pagina.getSize());
        assertEquals(2, pagina.getContent().size());
    }

    @Test
    void vistaPreviaDeReseniasNoAnunciaMasCuandoElTotalEsCinco() {
        ReseniaRepository repo = mock(ReseniaRepository.class);
        ReseniaService service = new ReseniaService(repo);
        List<ReseniaResponseDTO> resenias = resenias(5);

        when(repo.findPageReseniasPorSucursal(1, PageRequest.of(0, 5)))
                .thenReturn(new PageImpl<>(resenias, PageRequest.of(0, 5), 5));

        VistaPreviaDTO<ReseniaResponseDTO> vistaPrevia = service.getReseniasPorSucursal(1);

        assertEquals(5, vistaPrevia.getContent().size());
        assertEquals(5, vistaPrevia.getTotalElements());
        assertFalse(vistaPrevia.isHayMas());
    }

    @Test
    void solicitudesDelEmpleadorMuestranTresYElHistorialPaginaDeDiez() {
        SolicitudEmpleadorRepository repo = mock(SolicitudEmpleadorRepository.class);
        SolicitudEmpleadorService service = new SolicitudEmpleadorService(
                repo, mock(UsuarioService.class), mock(RolService.class),
                mock(NotificacionEventosService.class));

        when(repo.findPageByUsuarioEmail(EMAIL, PageRequest.of(0, 3)))
                .thenReturn(new PageImpl<>(solicitudesEmpleador(3), PageRequest.of(0, 3), 4));
        when(repo.findPageByUsuarioEmail(EMAIL, PageRequest.of(1, 10)))
                .thenReturn(new PageImpl<>(solicitudesEmpleador(4), PageRequest.of(1, 10), 14));

        VistaPreviaDTO<SolicitudEmpleadorMiniDTO> vista = service.listarSolicitudesPropias(EMAIL);
        assertEquals(3, vista.getContent().size());
        assertEquals(4, vista.getTotalElements());
        assertTrue(vista.isHayMas());

        PaginaDTO<SolicitudEmpleadorMiniDTO> pagina =
                service.listarSolicitudesPropiasPaginadas(EMAIL, "1");
        assertEquals(14, pagina.getTotalElements());
        assertEquals(2, pagina.getTotalPages());
        assertEquals(10, pagina.getSize());
        assertEquals(4, pagina.getContent().size());
    }

    @Test
    void vistaPreviaDelEmpleadorSeOcultaCuandoNoHayMas() {
        SolicitudEmpleadorRepository repo = mock(SolicitudEmpleadorRepository.class);
        SolicitudEmpleadorService service = new SolicitudEmpleadorService(
                repo, mock(UsuarioService.class), mock(RolService.class),
                mock(NotificacionEventosService.class));

        when(repo.findPageByUsuarioEmail(EMAIL, PageRequest.of(0, 3)))
                .thenReturn(new PageImpl<>(solicitudesEmpleador(2), PageRequest.of(0, 3), 2));

        VistaPreviaDTO<SolicitudEmpleadorMiniDTO> vista = service.listarSolicitudesPropias(EMAIL);
        assertFalse(vista.isHayMas());
        assertEquals(2, vista.getContent().size());
    }

    @Test
    void solicitudesRecibidasMuestranCincoYElHistorialPaginaDeDiez() {
        SolicitudEmpleadoRepository repo = mock(SolicitudEmpleadoRepository.class);
        SolicitudEmpleadoService service = new SolicitudEmpleadoService(
                repo, mock(SucursalService.class), mock(ReseniaService.class),
                mock(RolService.class), mock(UsuarioService.class));

        when(repo.findPageByEmpleadoEmail(EMAIL, PageRequest.of(0, 5)))
                .thenReturn(new PageImpl<>(solicitudesEmpleado(5), PageRequest.of(0, 5), 12));
        when(repo.findPageByEmpleadoEmail(EMAIL, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(solicitudesEmpleado(10), PageRequest.of(0, 10), 12));

        VistaPreviaDTO<SolicitudEmpleadoMiniPropiaDTO> vista =
                service.listarSolicitudesRecibidas(EMAIL);
        assertEquals(5, vista.getContent().size());
        assertTrue(vista.isHayMas());
        assertEquals(12, vista.getTotalElements());

        PaginaDTO<SolicitudEmpleadoMiniPropiaDTO> pagina =
                service.listarSolicitudesRecibidasPaginadas(EMAIL, "0");
        assertEquals(10, pagina.getContent().size());
        assertEquals(2, pagina.getTotalPages());
        assertEquals(10, pagina.getSize());
    }

    @Test
    void invitacionesEnviadasMuestranTresYElHistorialPaginaDeDiez() {
        SolicitudEmpleadoRepository repo = mock(SolicitudEmpleadoRepository.class);
        SucursalService sucursalService = mock(SucursalService.class);
        SolicitudEmpleadoService service = new SolicitudEmpleadoService(
                repo, sucursalService, mock(ReseniaService.class),
                mock(RolService.class), mock(UsuarioService.class));

        Sucursal sucursal = new Sucursal();
        sucursal.setId(7);
        when(sucursalService.listarSucursalPorId(7)).thenReturn(sucursal);
        when(sucursalService.esEmpleadorAca(sucursal, EMAIL)).thenReturn(true);
        when(repo.findPageMiniBySucursalId(7, PageRequest.of(0, 3)))
                .thenReturn(new PageImpl<>(solicitudesSucursal(3), PageRequest.of(0, 3), 14));
        when(repo.findPageMiniBySucursalId(7, PageRequest.of(1, 10)))
                .thenReturn(new PageImpl<>(solicitudesSucursal(4), PageRequest.of(1, 10), 14));

        VistaPreviaDTO<SolicitudEmpleadoMiniSucursalDTO> vista =
                service.listarSolicitudesEnviadas(7, EMAIL);
        assertEquals(3, vista.getContent().size());
        assertEquals(14, vista.getTotalElements());
        assertTrue(vista.isHayMas());

        PaginaDTO<SolicitudEmpleadoMiniSucursalDTO> pagina =
                service.listarSolicitudesEnviadasPaginadas(7, EMAIL, "1");
        assertEquals(14, pagina.getTotalElements());
        assertEquals(2, pagina.getTotalPages());
        assertEquals(1, pagina.getNumber());
        assertEquals(10, pagina.getSize());
        assertEquals(4, pagina.getContent().size());
    }

    @Test
    void invitacionesDeOtraSucursalNoSePaginan() {
        SolicitudEmpleadoRepository repo = mock(SolicitudEmpleadoRepository.class);
        SucursalService sucursalService = mock(SucursalService.class);
        SolicitudEmpleadoService service = new SolicitudEmpleadoService(
                repo, sucursalService, mock(ReseniaService.class),
                mock(RolService.class), mock(UsuarioService.class));

        Sucursal sucursal = new Sucursal();
        sucursal.setId(7);
        when(sucursalService.listarSucursalPorId(7)).thenReturn(sucursal);

        assertThrows(ForbiddenException.class,
                () -> service.listarSolicitudesEnviadas(7, EMAIL));
        assertThrows(ForbiddenException.class,
                () -> service.listarSolicitudesEnviadasPaginadas(7, EMAIL, "0"));
    }

    @Test
    void misTurnosPaginaDeDiezConOrdenYMetadata() {
        TurnoRepository repo = mock(TurnoRepository.class);
        TurnoService service = new TurnoService(
                repo, mock(UsuarioService.class), mock(ServicioService.class),
                mock(SucursalService.class), mock(ReseniaService.class),
                mock(NotificacionEventosService.class));

        when(repo.findPageByClienteEmail(EMAIL, PageRequest.of(1, 10)))
                .thenReturn(new PageImpl<>(turnos(4), PageRequest.of(1, 10), 14));

        PaginaDTO<TurnoMiniDTO> pagina = service.listarTurnosPorClientePaginado(EMAIL, "1");
        assertEquals(14, pagina.getTotalElements());
        assertEquals(2, pagina.getTotalPages());
        assertEquals(1, pagina.getNumber());
        assertEquals(10, pagina.getSize());
    }

    @Test
    void historialYTurnosAsignadosPaginaDeDiezYConservanPermisos() {
        TurnoRepository repo = mock(TurnoRepository.class);
        SucursalService sucursalService = mock(SucursalService.class);
        TurnoService service = new TurnoService(
                repo, mock(UsuarioService.class), mock(ServicioService.class),
                sucursalService, mock(ReseniaService.class),
                mock(NotificacionEventosService.class));

        Sucursal sucursal = new Sucursal();
        sucursal.setId(3);
        when(sucursalService.listarSucursalPorId(3)).thenReturn(sucursal);
        when(sucursalService.esEmpleadorAca(sucursal, EMAIL)).thenReturn(true);
        when(sucursalService.enSucursal(EMAIL, sucursal)).thenReturn(true);
        when(repo.findPageByServicioSucursalIdAndEstado(3, EstadoTurno.CONFIRMADO,
                PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(turnos(10), PageRequest.of(0, 10), 23));
        when(repo.findPageByEmpleadoEmailAndServicioSucursalIdAndEstado(
                EMAIL, 3, EstadoTurno.CONFIRMADO, PageRequest.of(1, 10)))
                .thenReturn(new PageImpl<>(turnos(3), PageRequest.of(1, 10), 13));

        PaginaDTO<TurnoMiniDTO> historial = service.listarTurnosPorSucursalPaginado(
                3, EMAIL, EstadoTurno.CONFIRMADO, "0");
        assertEquals(23, historial.getTotalElements());
        assertEquals(3, historial.getTotalPages());
        assertEquals(10, historial.getSize());

        PaginaDTO<TurnoMiniDTO> asignados = service.listarTurnosPropiosPorSucursalPaginado(
                3, EMAIL, EstadoTurno.CONFIRMADO, "1");
        assertEquals(13, asignados.getTotalElements());
        assertEquals(1, asignados.getNumber());
        assertEquals(10, asignados.getSize());
    }

    @Test
    void turnosDeOtraSucursalNoSePaginan() {
        TurnoRepository repo = mock(TurnoRepository.class);
        SucursalService sucursalService = mock(SucursalService.class);
        TurnoService service = new TurnoService(
                repo, mock(UsuarioService.class), mock(ServicioService.class),
                sucursalService, mock(ReseniaService.class),
                mock(NotificacionEventosService.class));

        Sucursal sucursal = new Sucursal();
        sucursal.setId(3);
        when(sucursalService.listarSucursalPorId(3)).thenReturn(sucursal);

        assertThrows(ForbiddenException.class, () -> service.listarTurnosPorSucursalPaginado(
                3, EMAIL, EstadoTurno.CONFIRMADO, "0"));
        assertThrows(ForbiddenException.class, () -> service.listarTurnosPropiosPorSucursalPaginado(
                3, EMAIL, EstadoTurno.CONFIRMADO, "0"));
    }

    @Test
    void usuariosPaginadosConservanLosFiltros() {
        UsuarioRepository repo = mock(UsuarioRepository.class);
        UsuarioService service = new UsuarioService(
                repo, mock(RolService.class), mock(ImagenService.class),
                mock(ReseniaService.class), mock(org.springframework.security.crypto.password.PasswordEncoder.class),
                mock(RefreshTokenRepository.class), mock(PasswordResetTokenRepository.class),
                mock(BloqueoUsuario.class), mock(VerificacionRepository.class),
                mock(com.gg.turnlook.features.turno.TurnoBajaService.class));

        when(repo.buscarMiniAdminPagina("Ana", "Gomez", true, PageRequest.of(1, 10)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 10), 21));

        PaginaDTO<?> pagina = service.listarUsuariosPaginados("Ana", "Gomez", true, "1");
        assertEquals(21, pagina.getTotalElements());
        assertEquals(3, pagina.getTotalPages());
        assertEquals(10, pagina.getSize());
        verify(repo).buscarMiniAdminPagina("Ana", "Gomez", true, PageRequest.of(1, 10));
    }

    @Test
    void sucursalesPublicasUsanDiezPorPaginaCuandoNoSeIndicaSize() {
        SucursalRepository repo = mock(SucursalRepository.class);
        SucursalDescubrimientoService service = new SucursalDescubrimientoService(
                repo, mock(TurnoRepository.class), mock(SucursalVisitaRepository.class),
                mock(UsuarioService.class), mock(SucursalService.class));

        when(repo.findIdsOperativasOrdenadasPorRanking(eq(null), eq(null), any()))
                .thenReturn(List.of());
        when(repo.contarOperativas(null, null)).thenReturn(0L);

        var pagina = service.listarPaginado(null, null, "0", null);
        assertEquals(0, pagina.getTotalElements());
        assertEquals(10, pagina.getSize());
        verify(repo).findIdsOperativasOrdenadasPorRanking(
                null, null, PageRequest.of(0, 10));
    }

    @Test
    void paginaNegativaEsErrorDelPedido() {
        ReseniaRepository repo = mock(ReseniaRepository.class);
        ReseniaService service = new ReseniaService(repo);
        assertThrows(BadRequestException.class,
                () -> service.listarReseniasPaginadas(1, "-1"));
    }

    private List<ReseniaResponseDTO> resenias(int cantidad) {
        return java.util.stream.IntStream.range(0, cantidad)
                .mapToObj(i -> new ReseniaResponseDTO(5, "comentario " + i, LocalDate.now()))
                .toList();
    }

    private List<SolicitudEmpleadorMiniDTO> solicitudesEmpleador(int cantidad) {
        return java.util.stream.IntStream.range(0, cantidad)
                .mapToObj(i -> new SolicitudEmpleadorMiniDTO(
                        i, "Ana", "Gomez", LocalDate.now(), EstadoSolicitud.PENDIENTE))
                .toList();
    }

    private List<SolicitudEmpleadoMiniPropiaDTO> solicitudesEmpleado(int cantidad) {
        return java.util.stream.IntStream.range(0, cantidad)
                .mapToObj(i -> new SolicitudEmpleadoMiniPropiaDTO(
                        i, "Sucursal " + i, LocalDate.now()))
                .toList();
    }

    private List<SolicitudEmpleadoMiniSucursalDTO> solicitudesSucursal(int cantidad) {
        return java.util.stream.IntStream.range(0, cantidad)
                .mapToObj(i -> new SolicitudEmpleadoMiniSucursalDTO(
                        i, "Ana", "Gomez", LocalDate.now()))
                .toList();
    }

    private List<TurnoMiniDTO> turnos(int cantidad) {
        return java.util.stream.IntStream.range(0, cantidad)
                .mapToObj(i -> new TurnoMiniDTO(
                        i, "Servicio", LocalDateTime.now(), EstadoTurno.CONFIRMADO, 5))
                .toList();
    }
}
