package com.gg.turnlook.features.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gg.turnlook.common.exception.BadRequestException;
import com.gg.turnlook.common.exception.ConflictException;
import com.gg.turnlook.common.exception.ForbiddenException;
import com.gg.turnlook.common.exception.NotFoundException;
import com.gg.turnlook.features.chat.dto.ConversacionCrearDTO;
import com.gg.turnlook.features.chat.dto.ConversacionDetalleDTO;
import com.gg.turnlook.features.chat.dto.ConversacionResumenDTO;
import com.gg.turnlook.features.chat.dto.MensajeCrearDTO;
import com.gg.turnlook.features.notificacion.NotificacionEventosService;
import com.gg.turnlook.features.sucursal.Sucursal;
import com.gg.turnlook.features.servicio.Servicio;
import com.gg.turnlook.features.turno.EstadoTurno;
import com.gg.turnlook.features.turno.Turno;
import com.gg.turnlook.features.turno.TurnoRepository;
import com.gg.turnlook.features.usuario.Usuario;
import com.gg.turnlook.features.usuario.UsuarioService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ChatPermisosTest {

    private final ConversacionRepository conversacionRepo = mock(ConversacionRepository.class);
    private final ConversacionParticipanteRepository participanteRepo =
            mock(ConversacionParticipanteRepository.class);
    private final MensajeRepository mensajeRepo = mock(MensajeRepository.class);
    private final UsuarioService usuarioService = mock(UsuarioService.class);
    private final TurnoRepository turnoRepo = mock(TurnoRepository.class);

    private ChatService service;

    private final Usuario cliente = mock(Usuario.class);
    private final Usuario empleado = mock(Usuario.class);
    private final Usuario empleador = mock(Usuario.class);
    private final Usuario tercero = mock(Usuario.class);
    private final Sucursal sucursal = mock(Sucursal.class);
    private final Turno turno = mock(Turno.class);
    private final Servicio servicio = mock(Servicio.class);
    private final Conversacion conversacion = mock(Conversacion.class);

    @BeforeEach
    void setUp() {
        ChatAccesoService acceso = new ChatAccesoService(conversacionRepo, participanteRepo);
        ChatMapper mapper = new ChatMapper();
        ConversacionService conversaciones = new ConversacionService(
                conversacionRepo, mensajeRepo, usuarioService, turnoRepo, acceso, mapper);
        MensajeService mensajes = new MensajeService(
                conversacionRepo, mensajeRepo, acceso, mapper,
                mock(NotificacionEventosService.class));
        service = new ChatService(conversaciones, mensajes);

        when(cliente.getId()).thenReturn(1);
        when(cliente.getEmail()).thenReturn("cliente@example.com");
        when(cliente.getNombre()).thenReturn("Cliente");
        when(cliente.getApellido()).thenReturn("Uno");

        when(empleado.getId()).thenReturn(4);
        when(empleado.getEmail()).thenReturn("empleado@example.com");
        when(empleado.getNombre()).thenReturn("Empleado");
        when(empleado.getApellido()).thenReturn("Cuatro");

        when(empleador.getId()).thenReturn(2);
        when(empleador.getEmail()).thenReturn("empleador@example.com");
        when(empleador.getNombre()).thenReturn("Empleador");
        when(empleador.getApellido()).thenReturn("Dos");

        when(tercero.getId()).thenReturn(3);
        when(tercero.getEmail()).thenReturn("tercero@example.com");

        when(sucursal.getId()).thenReturn(10);
        when(sucursal.getNombre()).thenReturn("Sucursal Centro");
        when(sucursal.getEmpleador()).thenReturn(empleador);

        when(servicio.getSucursal()).thenReturn(sucursal);
        when(turno.getId()).thenReturn(100);
        when(turno.getServicio()).thenReturn(servicio);
        when(turno.getCliente()).thenReturn(cliente);
        when(turno.getEmpleado()).thenReturn(empleado);

        when(conversacion.getId()).thenReturn(50);
        when(conversacion.getSucursal()).thenReturn(sucursal);
        when(conversacion.getTurno()).thenReturn(turno);
        when(conversacion.getCliente()).thenReturn(cliente);
    }

    private void prepararCreacion() {
        when(usuarioService.listarUsuarioPorEmail(anyString())).thenReturn(cliente);
        when(turnoRepo.findById(100)).thenReturn(Optional.of(turno));
        when(conversacionRepo.findByTurnoId(100)).thenReturn(Optional.empty());
        when(conversacionRepo.save(any(Conversacion.class))).thenAnswer(inv -> {
            Conversacion c = inv.getArgument(0);
            c.setId(50);
            return c;
        });
        when(participanteRepo.save(any(ConversacionParticipante.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(participanteRepo.findByConversacionId(50)).thenReturn(List.of());
    }

    private List<ConversacionParticipante> participantesGuardados() {
        ArgumentCaptor<ConversacionParticipante> captor =
                ArgumentCaptor.forClass(ConversacionParticipante.class);
        verify(participanteRepo, times(2)).save(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void crearConversacionDeTurnoRegistraClienteYEmpleadoAsignado() {
        ConversacionCrearDTO dto = new ConversacionCrearDTO();
        dto.setTurnoId(100);
        prepararCreacion();

        ConversacionDetalleDTO detalle = service.crear(dto, "cliente@example.com");

        assertEquals(50, detalle.getId());
        List<ConversacionParticipante> participantes = participantesGuardados();
        assertEquals(2, participantes.size());
        assertEquals(cliente, participantes.get(0).getUsuario());
        assertEquals(empleado, participantes.get(1).getUsuario());
    }

    @Test
    void rolesSeDerivanDelTurnoEnLaRespuesta() {
        ConversacionParticipante participanteCliente = mock(ConversacionParticipante.class);
        when(participanteCliente.getUsuario()).thenReturn(cliente);
        ConversacionParticipante participanteEmpleado = mock(ConversacionParticipante.class);
        when(participanteEmpleado.getUsuario()).thenReturn(empleado);
        when(conversacionRepo.findById(50)).thenReturn(Optional.of(conversacion));
        when(participanteRepo.findByConversacionIdAndUsuarioEmail(50, "cliente@example.com"))
                .thenReturn(Optional.of(participanteCliente));
        when(participanteRepo.findByConversacionId(50))
                .thenReturn(List.of(participanteCliente, participanteEmpleado));

        ConversacionDetalleDTO detalle = service.obtener(50, "cliente@example.com");

        assertEquals("CLIENTE", detalle.getParticipantes().get(0).getRol());
        assertEquals("EMPLEADO", detalle.getParticipantes().get(1).getRol());
    }

    @Test
    void sinEmpleadoAsignadoNoSePuedeCrearElChat() {
        when(turno.getEmpleado()).thenReturn(null);
        ConversacionCrearDTO dto = new ConversacionCrearDTO();
        dto.setTurnoId(100);
        when(usuarioService.listarUsuarioPorEmail("cliente@example.com")).thenReturn(cliente);
        when(turnoRepo.findById(100)).thenReturn(Optional.of(turno));

        assertThrows(ConflictException.class,
                () -> service.crear(dto, "cliente@example.com"));

        verify(participanteRepo, never()).save(any(ConversacionParticipante.class));
    }

    @Test
    void sinTurnoNoSePuedeCrearElChat() {
        ConversacionCrearDTO dto = new ConversacionCrearDTO();

        assertThrows(BadRequestException.class,
                () -> service.crear(dto, "cliente@example.com"));
    }

    @Test
    void empleadoPuedeAccederAlChatDelTurnoQueAtiende() {
        ConversacionParticipante participante = mock(ConversacionParticipante.class);
        when(participante.getUsuario()).thenReturn(empleado);
        when(conversacionRepo.findById(50)).thenReturn(Optional.of(conversacion));
        when(participanteRepo.findByConversacionIdAndUsuarioEmail(50, "empleado@example.com"))
                .thenReturn(Optional.of(participante));

        assertEquals(50, service.obtener(50, "empleado@example.com").getId());
    }

    @Test
    void empleadorPuedeAccederAlChatCuandoTambienEsElEmpleadoDelTurno() {
        when(turno.getEmpleado()).thenReturn(empleador);
        ConversacionParticipante participante = mock(ConversacionParticipante.class);
        when(participante.getUsuario()).thenReturn(empleador);
        when(conversacionRepo.findById(50)).thenReturn(Optional.of(conversacion));
        when(participanteRepo.findByConversacionIdAndUsuarioEmail(50, "empleador@example.com"))
                .thenReturn(Optional.of(participante));

        assertEquals(50, service.obtener(50, "empleador@example.com").getId());
    }

    @Test
    void empleadorAjenoAlTurnoNoPuedeCrearElChat() {
        when(usuarioService.listarUsuarioPorEmail("empleador@example.com")).thenReturn(empleador);
        when(turnoRepo.findById(100)).thenReturn(Optional.of(turno));

        ConversacionCrearDTO dto = new ConversacionCrearDTO();
        dto.setTurnoId(100);

        assertThrows(ForbiddenException.class,
                () -> service.crear(dto, "empleador@example.com"));
    }

    @Test
    void terceroNoParticipanteNoPuedeLeerLaConversacion() {
        when(conversacionRepo.findById(50)).thenReturn(Optional.of(conversacion));
        when(participanteRepo.findByConversacionIdAndUsuarioEmail(50, "tercero@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class,
                () -> service.obtener(50, "tercero@example.com"));
    }

    @Test
    void terceroNoParticipanteNoPuedeListarMensajes() {
        when(conversacionRepo.findById(50)).thenReturn(Optional.of(conversacion));
        when(participanteRepo.findByConversacionIdAndUsuarioEmail(50, "tercero@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class,
                () -> service.listarMensajes(50, null, null, 30, "tercero@example.com"));
    }

    @Test
    void terceroNoParticipanteNoPuedeEnviarMensajes() {
        when(conversacionRepo.findById(50)).thenReturn(Optional.of(conversacion));
        when(participanteRepo.findByConversacionIdAndUsuarioEmail(50, "tercero@example.com"))
                .thenReturn(Optional.empty());

        MensajeCrearDTO dto = new MensajeCrearDTO("hola", UUID.randomUUID());
        assertThrows(ForbiddenException.class,
                () -> service.enviar(50, dto, "tercero@example.com"));

        verify(mensajeRepo, never()).save(any(Mensaje.class));
    }

    @Test
    void conversacionInexistenteDevuelveNotFound() {
        when(conversacionRepo.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.obtener(99, "cliente@example.com"));
    }

    @Test
    void enviarMensajePersisteYActualizaUltimoMensaje() {
        ConversacionParticipante participante = mock(ConversacionParticipante.class);
        when(participante.getUsuario()).thenReturn(cliente);

        when(conversacionRepo.findById(50)).thenReturn(Optional.of(conversacion));
        when(participanteRepo.findByConversacionIdAndUsuarioEmail(50, "cliente@example.com"))
                .thenReturn(Optional.of(participante));

        UUID idCliente = UUID.randomUUID();
        MensajeCrearDTO dto = new MensajeCrearDTO("  hola  ", idCliente);
        when(mensajeRepo.findByConversacionIdAndClienteMensajeId(50, idCliente))
                .thenReturn(Optional.empty());
        when(mensajeRepo.save(any(Mensaje.class))).thenAnswer(inv -> {
            Mensaje m = inv.getArgument(0);
            m.setId(7L);
            return m;
        });

        service.enviar(50, dto, "cliente@example.com");

        verify(mensajeRepo).save(any(Mensaje.class));
        verify(conversacion).setUltimoMensajeId(7L);
        verify(conversacionRepo).save(conversacion);
    }

    @Test
    void reenvioConMismoClienteMensajeIdNoDuplica() {
        ConversacionParticipante participante = mock(ConversacionParticipante.class);
        when(participante.getUsuario()).thenReturn(cliente);

        UUID idCliente = UUID.randomUUID();
        Mensaje existente = mock(Mensaje.class);
        when(existente.getId()).thenReturn(7L);
        when(existente.getAutor()).thenReturn(cliente);
        when(existente.getConversacion()).thenReturn(conversacion);

        when(conversacionRepo.findById(50)).thenReturn(Optional.of(conversacion));
        when(participanteRepo.findByConversacionIdAndUsuarioEmail(any(), anyString()))
                .thenReturn(Optional.of(participante));
        when(mensajeRepo.findByConversacionIdAndClienteMensajeId(50, idCliente))
                .thenReturn(Optional.of(existente));

        service.enviar(50, new MensajeCrearDTO("hola", idCliente), "cliente@example.com");

        verify(mensajeRepo, never()).save(any(Mensaje.class));
    }

    @Test
    void listarIncluyeServicioClienteYProfesionalDelTurno() {
        when(servicio.getNombre()).thenReturn("Corte de cabello");

        ConversacionParticipante participante = participanteDeCliente();
        when(usuarioService.listarUsuarioPorEmail("cliente@example.com")).thenReturn(cliente);
        when(participanteRepo.findByUsuarioEmailOrderByConversacionIdDesc("cliente@example.com"))
                .thenReturn(List.of(participante));
        when(participanteRepo.findByConversacionId(50)).thenReturn(List.of(participante));
        when(mensajeRepo.countByConversacionIdAndAutorEmailNot(50, "cliente@example.com"))
                .thenReturn(0L);

        List<ConversacionResumenDTO> resumenes = service.listar("cliente@example.com");

        assertEquals(1, resumenes.size());
        ConversacionResumenDTO resumen = resumenes.get(0);
        assertEquals("Sucursal Centro", resumen.getSucursalNombre());
        assertEquals(100, resumen.getTurnoId());
        assertEquals("Corte de cabello", resumen.getServicioNombre());
        assertEquals("Cliente Uno", resumen.getClienteNombre());
        assertEquals("Empleado Cuatro", resumen.getProfesionalNombre());
    }

    @Test
    void detalleIncluyeServicioClienteYProfesionalDelTurno() {
        when(servicio.getNombre()).thenReturn("Corte de cabello");

        ConversacionParticipante participante = participanteDeCliente();
        when(conversacionRepo.findById(50)).thenReturn(Optional.of(conversacion));
        when(participanteRepo.findByConversacionIdAndUsuarioEmail(50, "cliente@example.com"))
                .thenReturn(Optional.of(participante));
        when(participanteRepo.findByConversacionId(50)).thenReturn(List.of(participante));

        ConversacionDetalleDTO detalle = service.obtener(50, "cliente@example.com");

        assertEquals("Corte de cabello", detalle.getServicioNombre());
        assertEquals("Cliente Uno", detalle.getClienteNombre());
        assertEquals("Empleado Cuatro", detalle.getProfesionalNombre());
    }

    @Test
    void conversacionSinTurnoNoExponeServicioNiProfesional() {
        ConversacionParticipante participante = participanteDeCliente();
        when(conversacion.getTurno()).thenReturn(null);
        when(usuarioService.listarUsuarioPorEmail("cliente@example.com")).thenReturn(cliente);
        when(participanteRepo.findByUsuarioEmailOrderByConversacionIdDesc("cliente@example.com"))
                .thenReturn(List.of(participante));
        when(participanteRepo.findByConversacionId(50)).thenReturn(List.of(participante));
        when(mensajeRepo.countByConversacionIdAndAutorEmailNot(50, "cliente@example.com"))
                .thenReturn(0L);

        ConversacionResumenDTO resumen = service.listar("cliente@example.com").get(0);

        assertNull(resumen.getTurnoId());
        assertNull(resumen.getServicioNombre());
        assertNull(resumen.getClienteNombre());
        assertNull(resumen.getProfesionalNombre());
    }

    @Test
    void listarOcultaConversacionFueraDeVentana() {
        when(turno.getEstado()).thenReturn(EstadoTurno.REALIZADO);
        when(turno.getFechaModificacionEstado()).thenReturn(LocalDateTime.now().minusDays(4));

        ConversacionParticipante participante = participanteDeCliente();
        when(usuarioService.listarUsuarioPorEmail("cliente@example.com")).thenReturn(cliente);
        when(participanteRepo.findByUsuarioEmailOrderByConversacionIdDesc("cliente@example.com"))
                .thenReturn(List.of(participante));
        when(participanteRepo.findByConversacionId(50)).thenReturn(List.of(participante));

        assertTrue(service.listar("cliente@example.com").isEmpty());
    }

    @Test
    void enviarFueraDeVentanaNoPersiste() {
        when(turno.getEstado()).thenReturn(EstadoTurno.REALIZADO);
        when(turno.getFechaModificacionEstado()).thenReturn(LocalDateTime.now().minusDays(4));

        ConversacionParticipante participante = mock(ConversacionParticipante.class);
        when(participante.getUsuario()).thenReturn(cliente);
        when(conversacionRepo.findById(50)).thenReturn(Optional.of(conversacion));
        when(participanteRepo.findByConversacionIdAndUsuarioEmail(50, "cliente@example.com"))
                .thenReturn(Optional.of(participante));

        MensajeCrearDTO dto = new MensajeCrearDTO("hola", UUID.randomUUID());
        assertThrows(ForbiddenException.class,
                () -> service.enviar(50, dto, "cliente@example.com"));
        verify(mensajeRepo, never()).save(any(Mensaje.class));
    }

    private ConversacionParticipante participanteDeCliente() {
        ConversacionParticipante participante = mock(ConversacionParticipante.class);
        when(participante.getConversacion()).thenReturn(conversacion);
        when(participante.getUsuario()).thenReturn(cliente);
        return participante;
    }
}
