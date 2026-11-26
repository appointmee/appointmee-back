package com.gg.turnlook.features.notificacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.gg.turnlook.features.servicio.Servicio;
import com.gg.turnlook.features.solicitud.empleador.SolicitudEmpleador;
import com.gg.turnlook.features.sucursal.Sucursal;
import com.gg.turnlook.features.turno.Turno;
import com.gg.turnlook.features.usuario.Usuario;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** US-NOT-04 / SCRUM-328: generación automática de notificaciones por evento. */
class NotificacionEventosServiceTest {

    private final NotificacionService notificacionService = mock(NotificacionService.class);
    private NotificacionEventosService service;

    @BeforeEach
    void setUp() {
        service = new NotificacionEventosService(notificacionService);
    }

    @Test
    void reservaAvisaAClienteProfesionalYEmpleadorSinRepetirDestinatarios() {
        Usuario cliente = usuario(1, "cliente@example.com");
        Usuario empleado = usuario(2, "empleado@example.com");
        Usuario empleador = usuario(3, "empleador@example.com");

        service.turnoReservado(turno(cliente, empleado, empleador));

        List<Registro> registros = registros();
        assertEquals(3, registros.size());
        assertEquals(List.of("cliente@example.com", "empleado@example.com", "empleador@example.com"),
                registros.stream().map(Registro::email).toList());
        assertTrue(registros.stream().allMatch(r -> r.tipo() == TipoNotificacion.RESERVA));
        assertTrue(registros.stream().allMatch(r -> r.destinoTipo() == TipoDestinoNotificacion.TURNO));
        assertTrue(registros.stream().allMatch(r -> r.destinoId().equals(41) && r.sucursalId().equals(7)));
        assertTrue(registros.stream().allMatch(r -> r.mensaje().toLowerCase().contains("turno")));
    }

    @Test
    void reservaNoDuplicaCuandoElEmpleadorEsElProfesionalAsignado() {
        Usuario cliente = usuario(1, "cliente@example.com");
        Usuario profesional = usuario(2, "empleador@example.com");

        service.turnoReservado(turno(cliente, profesional, profesional));

        assertEquals(List.of("cliente@example.com", "empleador@example.com"),
                registros().stream().map(Registro::email).toList());
    }

    @Test
    void cancelacionAvisaSoloALasPartesDelTurno() {
        Usuario cliente = usuario(1, "cliente@example.com");
        Usuario empleado = usuario(2, "empleado@example.com");
        Usuario empleador = usuario(3, "empleador@example.com");
        Usuario ajeno = usuario(9, "otro@example.com");

        service.turnoCancelado(turno(cliente, empleado, empleador));

        List<Registro> registros = registros();
        assertEquals(3, registros.size());
        assertTrue(registros.stream().allMatch(r -> r.tipo() == TipoNotificacion.CANCELACION));
        assertTrue(registros.stream().allMatch(r -> "Turno cancelado".equals(r.titulo())));
        assertFalse(registros.stream().anyMatch(r -> r.email().equals(ajeno.getEmail())));
    }

    @Test
    void finalizacionInformaAlClienteQuePuedeReseniar() {
        Usuario cliente = usuario(1, "cliente@example.com");
        Usuario empleado = usuario(2, "empleado@example.com");

        service.turnoFinalizado(turno(cliente, empleado, null));

        List<Registro> registros = registros();
        assertEquals(2, registros.size());
        Registro avisoCliente = registros.stream()
                .filter(r -> r.email().equals("cliente@example.com")).findFirst().orElseThrow();
        assertEquals("Turno finalizado", avisoCliente.titulo());
        assertTrue(avisoCliente.mensaje().contains("reseña"));
        assertTrue(registros.stream().allMatch(r -> r.tipo() == TipoNotificacion.RESERVA));
    }

    @Test
    void pagoAprobadoSoloNotificaAlClienteYDestinoEsPago() {
        Usuario cliente = usuario(1, "cliente@example.com");
        Usuario empleado = usuario(2, "empleado@example.com");

        service.pagoAprobado(turno(cliente, empleado, null));

        List<Registro> registros = registros();
        assertEquals(1, registros.size());
        assertEquals("cliente@example.com", registros.getFirst().email());
        assertEquals(TipoNotificacion.PAGO, registros.getFirst().tipo());
        assertEquals(TipoDestinoNotificacion.PAGO, registros.getFirst().destinoTipo());
        assertEquals("Pago aprobado", registros.getFirst().titulo());
    }

    @Test
    void pagoRechazadoSoloNotificaAlCliente() {
        Usuario cliente = usuario(1, "cliente@example.com");
        Usuario empleado = usuario(2, "empleado@example.com");

        service.pagoRechazado(turno(cliente, empleado, null));

        List<Registro> registros = registros();
        assertEquals(1, registros.size());
        assertEquals("cliente@example.com", registros.getFirst().email());
        assertEquals("Pago rechazado", registros.getFirst().titulo());
        assertTrue(registros.getFirst().mensaje().contains("intentar"));
    }

    @Test
    void reseñaPublicadaNotificaAlProfesionalNoAlCliente() {
        Usuario cliente = usuario(1, "cliente@example.com");
        Usuario empleado = usuario(2, "empleado@example.com");

        service.reseniaPublicada(turno(cliente, empleado, null));

        List<Registro> registros = registros();
        assertEquals(1, registros.size());
        assertEquals("empleado@example.com", registros.getFirst().email());
        assertEquals(TipoDestinoNotificacion.TURNO, registros.getFirst().destinoTipo());
        assertFalse(registros.getFirst().mensaje().contains("cliente@example.com"));
    }

    @Test
    void solicitudAprobadaYRechazadaNotificanSoloAlSolicitante() {
        Usuario solicitante = usuario(5, "solicitante@example.com");
        SolicitudEmpleador solicitud = new SolicitudEmpleador("Quiero ser empleador", solicitante);
        solicitud.setId(12);

        service.solicitudResuelta(solicitud, true);
        assertEquals("Solicitud aprobada", registros().getFirst().titulo());
        limpiarRegistros();

        service.solicitudResuelta(solicitud, false);
        List<Registro> rechazo = registros();
        assertEquals(1, rechazo.size());
        assertEquals("solicitante@example.com", rechazo.getFirst().email());
        assertEquals("Solicitud rechazada", rechazo.getFirst().titulo());
        assertEquals(TipoNotificacion.SOLICITUD, rechazo.getFirst().tipo());
        assertEquals(TipoDestinoNotificacion.SOLICITUD, rechazo.getFirst().destinoTipo());
        assertEquals(12, rechazo.getFirst().destinoId());
    }

    @Test
    void mensajeNuevoNotificaALosDestinatariosSinIncluirElContenido() {
        Usuario cliente = usuario(2, "cliente@example.com");
        Usuario otro = usuario(3, "empleado@example.com");

        // El propio MensajeService excluye al autor antes de invocar al generador.
        service.mensajeNuevo(31, List.of(cliente, otro));

        List<Registro> registros = registros();
        assertEquals(2, registros.size());
        assertEquals(List.of("cliente@example.com", "empleado@example.com"),
                registros.stream().map(Registro::email).toList());
        assertTrue(registros.stream().allMatch(r -> r.tipo() == TipoNotificacion.MENSAJE));
        assertTrue(registros.stream().allMatch(r -> r.destinoTipo() == TipoDestinoNotificacion.CONVERSACION));
        assertTrue(registros.stream().allMatch(r -> r.destinoId().equals(31)));
        assertTrue(registros.stream().allMatch(r -> !r.mensaje().contains("secreto del mensaje")));
    }

    @Test
    void losTextosNoExponenEmailsNombresNiDatosSensibles() {
        Usuario cliente = usuario(1, "cliente@example.com");
        Usuario empleado = usuario(2, "empleado@example.com");
        Turno turno = turno(cliente, empleado, null);

        service.turnoReservado(turno);
        service.turnoCancelado(turno);
        service.turnoFinalizado(turno);
        service.pagoAprobado(turno);
        service.pagoRechazado(turno);
        service.reseniaPublicada(turno);
        service.mensajeNuevo(31, List.of(cliente));

        assertTrue(registros().stream().allMatch(r ->
                !r.titulo().contains("@")
                        && !r.mensaje().contains("@")
                        && !r.titulo().contains("Ana")
                        && !r.mensaje().contains("contraseña")
                        && !r.mensaje().contains("token")));
    }

    @Test
    void unFalloAlNotificarNoInterrumpeALosDemasDestinatarios() {
        Usuario cliente = usuario(1, "cliente@example.com");
        Usuario empleado = usuario(2, "empleado@example.com");
        doThrow(new RuntimeException("base caída"))
                .when(notificacionService)
                .registrar(eq("cliente@example.com"), any(), anyString(), anyString(),
                        any(), any(), any());

        assertDoesNotThrow(() -> service.turnoReservado(turno(cliente, empleado, null)));

        // El destinatario fallido se intentó igual; el siguiente se notificó sin problemas.
        assertEquals(List.of("cliente@example.com", "empleado@example.com"),
                registros().stream().map(Registro::email).toList());
    }

    @Test
    void unFalloAlExtraerDatosDelEventoNoSePropaga() {
        assertDoesNotThrow(() -> service.turnoReservado(new Turno()));
        assertDoesNotThrow(() -> service.pagoAprobado(new Turno()));
        assertDoesNotThrow(() -> service.reseniaPublicada(new Turno()));
        assertDoesNotThrow(() -> service.mensajeNuevo(31, null));
    }

    private void limpiarRegistros() {
        clearInvocations(notificacionService);
    }

    private List<Registro> registros() {
        ArgumentCaptor<String> emails = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<TipoNotificacion> tipos = ArgumentCaptor.forClass(TipoNotificacion.class);
        ArgumentCaptor<String> titulos = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> mensajes = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<TipoDestinoNotificacion> destinos =
                ArgumentCaptor.forClass(TipoDestinoNotificacion.class);
        ArgumentCaptor<Integer> destinoIds = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> sucursalIds = ArgumentCaptor.forClass(Integer.class);

        verify(notificacionService, atLeast(0)).registrar(
                emails.capture(), tipos.capture(), titulos.capture(), mensajes.capture(),
                destinos.capture(), destinoIds.capture(), sucursalIds.capture());

        List<Registro> resultado = new ArrayList<>();
        for (int i = 0; i < emails.getAllValues().size(); i++) {
            resultado.add(new Registro(
                    emails.getAllValues().get(i),
                    tipos.getAllValues().get(i),
                    titulos.getAllValues().get(i),
                    mensajes.getAllValues().get(i),
                    destinos.getAllValues().get(i),
                    destinoIds.getAllValues().get(i),
                    sucursalIds.getAllValues().get(i)));
        }
        return resultado;
    }

    private Usuario usuario(Integer id, String email) {
        Usuario usuario = new Usuario("Ana", "Apellido", email, "hash");
        usuario.setId(id);
        return usuario;
    }

    private Turno turno(Usuario cliente, Usuario empleado, Usuario empleador) {
        Sucursal sucursal = new Sucursal();
        sucursal.setId(7);
        sucursal.setEmpleador(empleador);
        Servicio servicio = new Servicio();
        servicio.setId(3);
        servicio.setSucursal(sucursal);
        Turno turno = new Turno(LocalDateTime.now().plusDays(1), cliente, empleado, servicio);
        turno.setId(41);
        return turno;
    }

    private record Registro(String email, TipoNotificacion tipo, String titulo, String mensaje,
                            TipoDestinoNotificacion destinoTipo, Integer destinoId,
                            Integer sucursalId) {
    }
}
