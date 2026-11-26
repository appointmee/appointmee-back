package com.gg.turnlook.features.notificacion;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gg.turnlook.features.chat.ChatAccesoService;
import com.gg.turnlook.features.chat.ChatMapper;
import com.gg.turnlook.features.chat.Conversacion;
import com.gg.turnlook.features.chat.ConversacionParticipante;
import com.gg.turnlook.features.chat.Mensaje;
import com.gg.turnlook.features.chat.MensajeRepository;
import com.gg.turnlook.features.chat.MensajeService;
import com.gg.turnlook.features.chat.dto.MensajeCrearDTO;
import com.gg.turnlook.features.resenia.ReseniaService;
import com.gg.turnlook.features.servicio.Servicio;
import com.gg.turnlook.features.servicio.ServicioService;
import com.gg.turnlook.features.solicitud.EstadoSolicitud;
import com.gg.turnlook.features.solicitud.empleador.SolicitudEmpleador;
import com.gg.turnlook.features.solicitud.empleador.SolicitudEmpleadorRepository;
import com.gg.turnlook.features.solicitud.empleador.SolicitudEmpleadorService;
import com.gg.turnlook.features.sucursal.Sucursal;
import com.gg.turnlook.features.sucursal.SucursalService;
import com.gg.turnlook.features.turno.EstadoTurno;
import com.gg.turnlook.features.turno.Turno;
import com.gg.turnlook.features.turno.TurnoRepository;
import com.gg.turnlook.features.turno.TurnoService;
import com.gg.turnlook.features.turno.dto.TurnoCrearDTO;
import com.gg.turnlook.features.usuario.Usuario;
import com.gg.turnlook.features.usuario.UsuarioService;
import com.gg.turnlook.features.usuario.rol.ERol;
import com.gg.turnlook.features.usuario.rol.Rol;
import com.gg.turnlook.features.usuario.rol.RolService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Verifica que los módulos disparen el evento correcto y no se detengan por él. */
class NotificacionesPorEventosTest {

    private final NotificacionEventosService eventos = mock(NotificacionEventosService.class);

    @Test
    void reservaDisparaLaNotificacionDespuesDePersistirElTurno() {
        TurnoRepository turnoRepo = mock(TurnoRepository.class);
        UsuarioService usuarioService = mock(UsuarioService.class);
        ServicioService servicioService = mock(ServicioService.class);
        SucursalService sucursalService = mock(SucursalService.class);
        ReseniaService reseniaService = mock(ReseniaService.class);
        TurnoService service = new TurnoService(turnoRepo, usuarioService, servicioService,
                sucursalService, reseniaService, eventos);

        Usuario cliente = usuario(1, "cliente@example.com");
        Usuario empleado = usuario(2, "empleado@example.com");
        Rol rol = new Rol();
        rol.setRol(ERol.EMPLEADO);
        empleado.setRoles(Set.of(rol));

        Servicio servicio = mock(Servicio.class);
        Sucursal sucursal = mock(Sucursal.class);
        when(servicio.getSucursal()).thenReturn(sucursal);
        when(servicio.getDuracion()).thenReturn(30);
        when(sucursal.abreElDia(any())).thenReturn(true);
        when(sucursal.getHoraApertura()).thenReturn(LocalTime.of(9, 0));
        when(sucursal.getHoraCierre()).thenReturn(LocalTime.of(18, 0));
        when(usuarioService.listarUsuarioPorIdConRoles(2)).thenReturn(empleado);
        when(usuarioService.listarUsuarioPorEmail("cliente@example.com")).thenReturn(cliente);
        when(servicioService.listarServicioPorId(3)).thenReturn(servicio);
        when(sucursalService.enSucursal("empleado@example.com", sucursal)).thenReturn(true);
        when(turnoRepo.findDisponibilidadPorEmpleado(eq(empleado), any(), any(), anyCollection()))
                .thenReturn(List.of());

        TurnoCrearDTO dto = new TurnoCrearDTO();
        dto.setFechaHora(LocalDate.now().plusDays(1).atTime(10, 0));
        dto.setEmpleadoId(2);
        dto.setServicioId(3);

        assertDoesNotThrow(() -> service.registrarTurno(dto, "cliente@example.com"));

        verify(turnoRepo).save(any(Turno.class));
        verify(eventos).turnoReservado(any(Turno.class));
    }

    @Test
    void pagoAprobadoNotificaSoloCuandoElEstadoCambia() {
        TurnoRepository turnoRepo = mock(TurnoRepository.class);
        TurnoService service = new TurnoService(turnoRepo, mock(UsuarioService.class),
                mock(ServicioService.class), mock(SucursalService.class),
                mock(ReseniaService.class), eventos);
        Turno turno = turno(1, 2);

        when(turnoRepo.actualizarEstadoSiEs(eq(41), anyCollection(), eq(EstadoTurno.CONFIRMADO)))
                .thenReturn(1);
        when(turnoRepo.findEstadoById(41)).thenReturn(Optional.of(EstadoTurno.CONFIRMADO));
        when(turnoRepo.findByIdConDetalles(41)).thenReturn(Optional.of(turno));

        assertEquals(EstadoTurno.CONFIRMADO, service.setearPagoAprobado(41));
        verify(eventos).pagoAprobado(turno);

        when(turnoRepo.actualizarEstadoSiEs(eq(41), anyCollection(), eq(EstadoTurno.CONFIRMADO)))
                .thenReturn(0);
        service.setearPagoAprobado(41);
        verify(eventos, times(1)).pagoAprobado(any());
    }

    @Test
    void solicitudAprobadaSePersisteAunqueLaNotificacionFalle() {
        SolicitudEmpleadorRepository repo = mock(SolicitudEmpleadorRepository.class);
        UsuarioService usuarioService = mock(UsuarioService.class);
        RolService rolService = mock(RolService.class);
        NotificacionService notificacionService = mock(NotificacionService.class);
        NotificacionEventosService generador = new NotificacionEventosService(notificacionService);
        SolicitudEmpleadorService service = new SolicitudEmpleadorService(
                repo, usuarioService, rolService, generador);

        SolicitudEmpleador solicitud = new SolicitudEmpleador("Motivo", usuario(5, "s@example.com"));
        solicitud.setId(9);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        when(repo.findById(9)).thenReturn(Optional.of(solicitud));
        when(rolService.listarPorRol(ERol.EMPLEADOR)).thenReturn(new Rol());
        doThrow(new RuntimeException("base caída"))
                .when(notificacionService)
                .registrar(anyString(), any(), anyString(), anyString(), any(), any(), any());

        assertDoesNotThrow(() -> service.aprobarSolicitud(9));

        assertEquals(EstadoSolicitud.APROBADA, solicitud.getEstado());
        verify(repo).save(solicitud);
        verify(notificacionService).registrar(eq("s@example.com"), any(), anyString(),
                anyString(), any(), any(), any());
    }

    @Test
    void solicitudRechazadaDisparaSuEvento() {
        SolicitudEmpleadorRepository repo = mock(SolicitudEmpleadorRepository.class);
        SolicitudEmpleadorService service = new SolicitudEmpleadorService(
                repo, mock(UsuarioService.class), mock(RolService.class), eventos);

        SolicitudEmpleador solicitud = new SolicitudEmpleador("Motivo", usuario(5, "s@example.com"));
        solicitud.setId(9);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        when(repo.findById(9)).thenReturn(Optional.of(solicitud));

        service.rechazarSolicitud(9);

        assertEquals(EstadoSolicitud.RECHAZADA, solicitud.getEstado());
        verify(eventos).solicitudResuelta(solicitud, false);
        verify(eventos, never()).solicitudResuelta(solicitud, true);
    }

    @Test
    void mensajeNuevoAvisaSoloALaOtraParte() {
        Conversacion conversacion = mock(Conversacion.class);
        when(conversacion.getId()).thenReturn(31);
        ConversacionParticipante participanteAutor = participante(1, "autor@example.com");
        ConversacionParticipante participanteCliente = participante(2, "cliente@example.com");
        ConversacionParticipante participanteEmpleado = participante(3, "empleado@example.com");

        ChatAccesoService acceso = mock(ChatAccesoService.class);
        when(acceso.buscarConversacion(31)).thenReturn(conversacion);
        when(acceso.participante(31, "autor@example.com")).thenReturn(participanteAutor);
        when(acceso.participantes(31)).thenReturn(List.of(
                participanteAutor, participanteCliente, participanteEmpleado));

        UUID clienteMensajeId = UUID.randomUUID();
        MensajeRepository mensajeRepo = mock(MensajeRepository.class);
        when(mensajeRepo.findByConversacionIdAndClienteMensajeId(31, clienteMensajeId))
                .thenReturn(Optional.empty());
        when(mensajeRepo.save(any(Mensaje.class))).thenAnswer(invocacion -> {
            Mensaje mensaje = invocacion.getArgument(0);
            mensaje.setId(77L);
            return mensaje;
        });

        MensajeService service = new MensajeService(mock(com.gg.turnlook.features.chat.ConversacionRepository.class),
                mensajeRepo, acceso, new ChatMapper(), eventos);

        MensajeCrearDTO dto = new MensajeCrearDTO();
        dto.setContenido("Hola, ¿sigue disponible el turno?");
        dto.setClienteMensajeId(clienteMensajeId);
        service.enviar(31, dto, "autor@example.com");

        ArgumentCaptor<List<Usuario>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventos).mensajeNuevo(eq(31), captor.capture());
        assertEquals(List.of("cliente@example.com", "empleado@example.com"),
                captor.getValue().stream().map(Usuario::getEmail).toList());
    }

    private ConversacionParticipante participante(Integer id, String email) {
        ConversacionParticipante participante = new ConversacionParticipante();
        participante.setUsuario(usuario(id, email));
        return participante;
    }

    private Usuario usuario(Integer id, String email) {
        Usuario usuario = new Usuario("Ana", "Apellido", email, "hash");
        usuario.setId(id);
        return usuario;
    }

    private Turno turno(Integer clienteId, Integer empleadoId) {
        return new Turno(LocalDateTime.now().plusDays(1),
                usuario(clienteId, "cliente@example.com"),
                usuario(empleadoId, "empleado@example.com"), null);
    }
}
