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
import com.gg.turnlook.features.notificacion.dto.PaginaNotificacionesDTO;
import com.gg.turnlook.features.usuario.Usuario;
import com.gg.turnlook.features.usuario.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class NotificacionServiceTest {

    private final NotificacionRepository notificacionRepository = mock(NotificacionRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);

    private NotificacionService service;

    @BeforeEach
    void setUp() {
        service = new NotificacionService(notificacionRepository, usuarioRepository);
    }

    @Test
    void listaConElEmailAutenticadoNormalizadoYElContadorDeLaBase() {
        when(notificacionRepository.listarPorUsuario(eq("ana@example.com"), eq(null), any()))
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
                .listarPorUsuario(eq("ana@example.com"), eq(null), any(Pageable.class));
        verify(notificacionRepository).contarNoLeidas("ana@example.com");
    }

    @Test
    void propagaElFiltroDeEstadoDeLecturaAlRepositorio() {
        when(notificacionRepository.listarPorUsuario(anyString(), eq(true), any()))
                .thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(20), 0));

        service.listar("ana@example.com", true, 0, 20);

        verify(notificacionRepository).listarPorUsuario(eq("ana@example.com"), eq(true), any());
        verify(notificacionRepository, never()).listarPorUsuario(anyString(), eq(false), any());
    }

    @Test
    void corrigePaginaNegativaYTopaElTamanoMaximo() {
        when(notificacionRepository.listarPorUsuario(anyString(), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(20), 0));

        service.listar("ana@example.com", null, -3, 500);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(notificacionRepository).listarPorUsuario(eq("ana@example.com"), eq(null),
                captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(NotificacionService.TAMANO_MAXIMO, captor.getValue().getPageSize());
    }

    @Test
    void usaElTamanoPorDefectoCuandoNoEsValido() {
        when(notificacionRepository.listarPorUsuario(anyString(), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(20), 0));

        service.listar("ana@example.com", null, 0, -1);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(notificacionRepository).listarPorUsuario(eq("ana@example.com"), eq(null),
                captor.capture());
        assertEquals(NotificacionService.TAMANO_DEFECTO, captor.getValue().getPageSize());
    }

    @Test
    void rechazaElListadoSinUsuarioAutenticado() {
        assertThrows(UnauthorizedException.class, () -> service.listar(null, null, 0, 20));
        assertThrows(UnauthorizedException.class, () -> service.listar("  ", null, 0, 20));
        assertThrows(UnauthorizedException.class, () -> service.contarNoLeidas(null));
        verify(notificacionRepository, never()).listarPorUsuario(anyString(), any(), any());
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
