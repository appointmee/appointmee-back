package com.gg.turnlook.features.notificacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gg.turnlook.common.exception.BadRequestException;
import com.gg.turnlook.common.exception.NotFoundException;
import com.gg.turnlook.common.exception.UnauthorizedException;
import com.gg.turnlook.features.notificacion.dto.ContadorNoLeidasDTO;
import com.gg.turnlook.features.notificacion.dto.PaginaNotificacionesDTO;
import com.gg.turnlook.features.usuario.Usuario;
import com.gg.turnlook.features.usuario.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.data.domain.Pageable;

class NotificacionServiceTest {

    private final NotificacionRepository notificacionRepository = mock(NotificacionRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);

    private NotificacionService service;

    @BeforeEach
    void setUp() {
        service = new NotificacionService(notificacionRepository, usuarioRepository,
                messagingTemplate);
    }

    @Test
    void listaConElEmailAutenticadoNormalizadoYElContadorDeLaBase() {
        when(notificacionRepository.listarPorUsuario(eq("ana@example.com"), eq(null), eq(null), any()))
                .thenReturn(new PageImpl<>(
                        List.of(notificacion(1, "Turno confirmado")), Pageable.ofSize(20), 1));
        when(notificacionRepository.contarNoLeidas("ana@example.com")).thenReturn(2L);

        PaginaNotificacionesDTO pagina = service.listar(" Ana@Example.com ", null, 0, 20);

        assertEquals(1, pagina.getContenido().size());
        assertEquals("Turno confirmado", pagina.getContenido().getFirst().getTitulo());
        assertEquals(2, pagina.getNoLeidas());
        assertEquals(0, pagina.getPagina());
        assertEquals(20, pagina.getTamano());
        assertEquals(1, pagina.getTotalElementos());
        assertEquals(1, pagina.getTotalPaginas());
        verify(notificacionRepository)
                .listarPorUsuario(eq("ana@example.com"), eq(null), eq(null), any(Pageable.class));
        verify(notificacionRepository).contarNoLeidas("ana@example.com");
    }

    @Test
    void propagaElFiltroDeEstadoDeLecturaAlRepositorio() {
        when(notificacionRepository.listarPorUsuario(anyString(), eq(null), eq(true), any()))
                .thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(20), 0));

        service.listar("ana@example.com", true, 0, 20);

        verify(notificacionRepository)
                .listarPorUsuario(eq("ana@example.com"), eq(null), eq(true), any());
        verify(notificacionRepository, never())
                .listarPorUsuario(anyString(), eq(null), eq(false), any());
    }

    @Test
    void combinaElFiltroDeTipoConElDeEstadoSinPerderResultados() {
        when(notificacionRepository.listarPorUsuario(
                "ana@example.com", TipoNotificacion.PAGO, false, Pageable.ofSize(20)))
                .thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(20), 0));

        service.listar("ana@example.com", TipoNotificacion.PAGO, false, 0, 20);

        verify(notificacionRepository).listarPorUsuario(
                "ana@example.com", TipoNotificacion.PAGO, false, Pageable.ofSize(20));
    }

    @Test
    void marcaUnaNotificacionPropiaYDevuelveElContadorActualizado() {
        Notificacion notificacion = notificacion(7, "Turno confirmado");
        when(notificacionRepository.findByIdAndUsuarioEmail(7, "ana@example.com"))
                .thenReturn(Optional.of(notificacion));
        when(notificacionRepository.contarNoLeidas("ana@example.com")).thenReturn(3L);

        ContadorNoLeidasDTO resultado = service.marcarLeida(" ANA@example.com ", 7);

        assertTrue(notificacion.isLeida());
        assertEquals(3, resultado.getNoLeidas());
        verify(notificacionRepository).save(notificacion);
    }

    @Test
    void marcarUnaNotificacionYaLeidaNoEsUnErrorYNoGuarda() {
        Notificacion notificacion = notificacion(7, "Turno confirmado");
        notificacion.setLeida(true);
        when(notificacionRepository.findByIdAndUsuarioEmail(7, "ana@example.com"))
                .thenReturn(Optional.of(notificacion));
        when(notificacionRepository.contarNoLeidas("ana@example.com")).thenReturn(0L);

        assertEquals(0, service.marcarLeida("ana@example.com", 7).getNoLeidas());

        verify(notificacionRepository, never()).save(any(Notificacion.class));
    }

    @Test
    void noPermiteMarcarUnaNotificacionDeOtroUsuario() {
        when(notificacionRepository.findByIdAndUsuarioEmail(7, "ana@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.marcarLeida("ana@example.com", 7));
        verify(notificacionRepository, never()).save(any(Notificacion.class));
    }

    @Test
    void rechazaMarcarSinUsuarioAutenticadoOConIdInvalido() {
        assertThrows(UnauthorizedException.class, () -> service.marcarLeida(null, 7));
        assertThrows(UnauthorizedException.class, () -> service.marcarLeida("  ", 7));
        assertThrows(BadRequestException.class, () -> service.marcarLeida("ana@example.com", null));
        assertThrows(BadRequestException.class, () -> service.marcarLeida("ana@example.com", 0));
        verify(notificacionRepository, never()).findByIdAndUsuarioEmail(any(), anyString());
    }

    @Test
    void marcaTodasLasPropiasDelTipoYDevuelveElContadorEnCero() {
        when(notificacionRepository.contarNoLeidas("ana@example.com")).thenReturn(0L);

        ContadorNoLeidasDTO resultado =
                service.marcarTodasLeidas(" ANA@Example.com ", TipoNotificacion.RESERVA);

        assertEquals(0, resultado.getNoLeidas());
        verify(notificacionRepository)
                .marcarTodasLeidas("ana@example.com", TipoNotificacion.RESERVA);
    }

    @Test
    void rechazaMarcarTodasSinUsuarioAutenticado() {
        assertThrows(UnauthorizedException.class, () -> service.marcarTodasLeidas(null, null));
        verify(notificacionRepository, never()).marcarTodasLeidas(anyString(), any());
    }

    @Test
    void corrigePaginaNegativaYTopaElTamanoMaximo() {
        when(notificacionRepository.listarPorUsuario(anyString(), eq(null), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(20), 0));

        service.listar("ana@example.com", null, -3, 500);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(notificacionRepository).listarPorUsuario(eq("ana@example.com"), eq(null), eq(null),
                captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(NotificacionService.TAMANO_MAXIMO, captor.getValue().getPageSize());
    }

    @Test
    void usaElTamanoPorDefectoCuandoNoEsValido() {
        when(notificacionRepository.listarPorUsuario(anyString(), eq(null), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(20), 0));

        service.listar("ana@example.com", null, 0, -1);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(notificacionRepository).listarPorUsuario(eq("ana@example.com"), eq(null), eq(null),
                captor.capture());
        assertEquals(NotificacionService.TAMANO_DEFECTO, captor.getValue().getPageSize());
    }

    @Test
    void rechazaElListadoSinUsuarioAutenticado() {
        assertThrows(UnauthorizedException.class, () -> service.listar(null, null, 0, 20));
        assertThrows(UnauthorizedException.class, () -> service.listar("  ", null, 0, 20));
        assertThrows(UnauthorizedException.class, () -> service.contarNoLeidas(null));
        verify(notificacionRepository, never()).listarPorUsuario(anyString(), any(), any(), any());
    }

    @Test
    void cuentaNoLeidasNormalizandoElEmail() {
        when(notificacionRepository.contarNoLeidas("ana@example.com")).thenReturn(7L);

        assertEquals(7, service.contarNoLeidas("ANA@Example.com").getNoLeidas());
        verify(notificacionRepository).contarNoLeidas("ana@example.com");
    }

    @Test
    void registraUnaNotificacionNuevaNoLeida() {
        Usuario ana = usuario(1, "ana@example.com");
        when(notificacionRepository.save(any(Notificacion.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        var dto = service.registrar(ana, TipoNotificacion.RESERVA,
                "  Turno confirmado  ", "  Tu turno quedó confirmado.  ");

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        Notificacion guardada = captor.getValue();
        assertEquals(ana, guardada.getUsuario());
        assertEquals(TipoNotificacion.RESERVA, guardada.getTipo());
        assertEquals("Turno confirmado", guardada.getTitulo());
        assertEquals("Tu turno quedó confirmado.", guardada.getMensaje());
        assertFalse(guardada.isLeida());
        assertEquals("Turno confirmado", dto.getTitulo());
        assertFalse(dto.isLeida());
    }

    @Test
    void publicaLaNotificacionNuevaAlCanalPrivadoDelUsuario() {
        Usuario ana = usuario(1, "ANA@Example.com");
        when(notificacionRepository.save(any(Notificacion.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        var dto = service.registrar(ana, TipoNotificacion.SISTEMA, "Aviso", "Novedad");

        verify(messagingTemplate).convertAndSend("/topic/notificaciones/ana@example.com", dto);
    }

    @Test
    void guardaElDestinoConcretoEnLaEntidadYEnElDTO() {
        Usuario ana = usuario(1, "ana@example.com");
        when(notificacionRepository.save(any(Notificacion.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        var dto = service.registrar(ana, TipoNotificacion.PAGO, "Pago aprobado",
                "Tu pago fue aprobado.", TipoDestinoNotificacion.PAGO, 77, 7);

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        Notificacion guardada = captor.getValue();
        assertEquals(TipoDestinoNotificacion.PAGO, guardada.getDestinoTipo());
        assertEquals(77, guardada.getDestinoId());
        assertEquals(7, guardada.getSucursalId());
        assertEquals(TipoDestinoNotificacion.PAGO, dto.getDestinoTipo());
        assertEquals(77, dto.getDestinoId());
        assertEquals(7, dto.getSucursalId());
    }

    @Test
    void rechazaUnDestinoSinIdentificador() {
        Usuario ana = usuario(1, "ana@example.com");

        assertThrows(BadRequestException.class,
                () -> service.registrar(ana, TipoNotificacion.RESERVA, "Turno", "Nuevo turno",
                        TipoDestinoNotificacion.TURNO, null, null));
        verify(notificacionRepository, never()).save(any(Notificacion.class));
    }

    @Test
    void registraBuscandoAlDestinatarioActivoPorEmail() {
        Usuario ana = usuario(1, "ana@example.com");
        when(usuarioRepository.findByEmailAndActivoTrue("ana@example.com"))
                .thenReturn(Optional.of(ana));
        when(notificacionRepository.save(any(Notificacion.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        service.registrar(" ANA@example.com ", TipoNotificacion.PAGO, "Pago", "Aprobado");

        verify(usuarioRepository).findByEmailAndActivoTrue("ana@example.com");
        verify(notificacionRepository).save(any(Notificacion.class));
    }

    @Test
    void rechazaUnRegistroIncompletoODemasiadoLargo() {
        Usuario ana = usuario(1, "ana@example.com");

        assertThrows(BadRequestException.class,
                () -> service.registrar((Usuario) null, TipoNotificacion.SISTEMA,
                        "Título", "Mensaje"));
        assertThrows(BadRequestException.class,
                () -> service.registrar((String) null, TipoNotificacion.SISTEMA,
                        "Título", "Mensaje"));
        assertThrows(BadRequestException.class,
                () -> service.registrar(new Usuario(), TipoNotificacion.SISTEMA, "Título", "Mensaje"));
        assertThrows(BadRequestException.class,
                () -> service.registrar(ana, null, "Título", "Mensaje"));
        assertThrows(BadRequestException.class,
                () -> service.registrar(ana, TipoNotificacion.SISTEMA, " ", "Mensaje"));
        assertThrows(BadRequestException.class,
                () -> service.registrar(ana, TipoNotificacion.SISTEMA, "Título", " "));
        assertThrows(BadRequestException.class,
                () -> service.registrar(ana, TipoNotificacion.SISTEMA, "x".repeat(121), "Mensaje"));
        assertThrows(BadRequestException.class,
                () -> service.registrar(ana, TipoNotificacion.SISTEMA, "Título", "x".repeat(501)));
        verify(notificacionRepository, never()).save(any(Notificacion.class));
    }

    @Test
    void noRegistraParaUnUsuarioQueNoExiste() {
        when(usuarioRepository.findByEmailAndActivoTrue("ana@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.registrar("ana@example.com", TipoNotificacion.SISTEMA,
                        "Título", "Mensaje"));
        verify(notificacionRepository, never()).save(any(Notificacion.class));
    }

    private Notificacion notificacion(Integer id, String titulo) {
        Notificacion notificacion = new Notificacion();
        notificacion.setId(id);
        notificacion.setTipo(TipoNotificacion.RESERVA);
        notificacion.setTitulo(titulo);
        notificacion.setMensaje("Mensaje");
        notificacion.setLeida(false);
        return notificacion;
    }

    private Usuario usuario(Integer id, String email) {
        Usuario usuario = new Usuario("Ana", "Paz", email, "hash");
        usuario.setId(id);
        return usuario;
    }
}
