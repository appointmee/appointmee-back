package com.gg.turnlook.features.notificacion;

import com.gg.turnlook.features.solicitud.empleador.SolicitudEmpleador;
import com.gg.turnlook.features.turno.Turno;
import com.gg.turnlook.features.usuario.Usuario;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Genera las notificaciones in-app derivadas de eventos de negocio (US-NOT-04 / SCRUM-328).
 *
 * <p>Es el único lugar donde se definen los textos y los destinatarios de cada evento.
 * Los módulos que producen el evento sólo invocan el método correspondiente después de
 * completar su operación. Ningún fallo al notificar se propaga: la operación original
 * ya se persistió y no debe verse afectada.</p>
 */
@Service
@RequiredArgsConstructor
public class NotificacionEventosService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionEventosService.class);

    private final NotificacionService notificacionService;

    /** Reserva nueva: avisa a cliente, profesional asignado y empleador de la sucursal. */
    public void turnoReservado(Turno turno) {
        ejecutar("turno reservado", () -> {
            Integer turnoId = turno.getId();
            Integer sucursalId = sucursalId(turno);
            Set<String> avisados = new HashSet<>();

            avisar(turno.getCliente(), TipoNotificacion.RESERVA,
                    "Tu reserva fue registrada",
                    "Tu turno quedó reservado. Te avisaremos cuando se confirme el pago.",
                    TipoDestinoNotificacion.TURNO, turnoId, sucursalId, avisados);
            avisar(turno.getEmpleado(), TipoNotificacion.RESERVA,
                    "Nueva reserva",
                    "Tenés un nuevo turno reservado. Revisá tu agenda.",
                    TipoDestinoNotificacion.TURNO, turnoId, sucursalId, avisados);
            avisar(empleador(turno), TipoNotificacion.RESERVA,
                    "Nueva reserva en tu sucursal",
                    "Se registró un nuevo turno en tu sucursal.",
                    TipoDestinoNotificacion.TURNO, turnoId, sucursalId, avisados);
        });
    }

    /** Cancelación de turno: avisa a las partes del turno. */
    public void turnoCancelado(Turno turno) {
        ejecutar("turno cancelado", () -> {
            Integer turnoId = turno.getId();
            Integer sucursalId = sucursalId(turno);
            Set<String> avisados = new HashSet<>();

            avisar(turno.getCliente(), TipoNotificacion.CANCELACION,
                    "Turno cancelado", "Tu turno fue cancelado.",
                    TipoDestinoNotificacion.TURNO, turnoId, sucursalId, avisados);
            avisar(turno.getEmpleado(), TipoNotificacion.CANCELACION,
                    "Turno cancelado", "El turno asignado fue cancelado.",
                    TipoDestinoNotificacion.TURNO, turnoId, sucursalId, avisados);
            avisar(empleador(turno), TipoNotificacion.CANCELACION,
                    "Turno cancelado", "Se canceló un turno de tu sucursal.",
                    TipoDestinoNotificacion.TURNO, turnoId, sucursalId, avisados);
        });
    }

    /** Turno finalizado: el cliente puede reseñar y el profesional ve el cierre del turno. */
    public void turnoFinalizado(Turno turno) {
        ejecutar("turno finalizado", () -> {
            Integer turnoId = turno.getId();
            Integer sucursalId = sucursalId(turno);
            Set<String> avisados = new HashSet<>();

            avisar(turno.getCliente(), TipoNotificacion.RESERVA,
                    "Turno finalizado", "Tu turno finalizó. Ya podés dejar una reseña.",
                    TipoDestinoNotificacion.TURNO, turnoId, sucursalId, avisados);
            avisar(turno.getEmpleado(), TipoNotificacion.RESERVA,
                    "Turno finalizado", "El turno finalizó correctamente.",
                    TipoDestinoNotificacion.TURNO, turnoId, sucursalId, avisados);
            avisar(empleador(turno), TipoNotificacion.RESERVA,
                    "Turno finalizado", "Un turno de tu sucursal finalizó correctamente.",
                    TipoDestinoNotificacion.TURNO, turnoId, sucursalId, avisados);
        });
    }

    /**
     * Pago aprobado: sólo al cliente que pagó, dueño de la operación de cobro.
     * El proveedor se resuelve dentro del bloque seguro para que una falla al cargar
     * los datos de la notificación no afecte al webhook que la disparó.
     */
    public void pagoAprobado(Supplier<Turno> proveedor) {
        ejecutar("pago aprobado", () -> {
            Turno turno = proveedor.get();
            avisar(turno.getCliente(), TipoNotificacion.PAGO,
                    "Pago aprobado", "Tu pago fue aprobado y el turno quedó confirmado.",
                    TipoDestinoNotificacion.PAGO, turno.getId(), sucursalId(turno),
                    new HashSet<>());
        });
    }

    /** Pago rechazado: sólo al cliente, para que pueda reintentar. */
    public void pagoRechazado(Supplier<Turno> proveedor) {
        ejecutar("pago rechazado", () -> {
            Turno turno = proveedor.get();
            avisar(turno.getCliente(), TipoNotificacion.PAGO,
                    "Pago rechazado", "No se pudo procesar tu pago. Podés intentar nuevamente.",
                    TipoDestinoNotificacion.PAGO, turno.getId(), sucursalId(turno),
                    new HashSet<>());
        });
    }

    /** Reseña publicada: al profesional que desarrolló el turno. */
    public void reseniaPublicada(Turno turno) {
        ejecutar("reseña publicada", () ->
                avisar(turno.getEmpleado(), TipoNotificacion.SISTEMA,
                        "Tu turno fue reseñado",
                        "El cliente publicó una reseña sobre el turno realizado.",
                        TipoDestinoNotificacion.TURNO, turno.getId(), sucursalId(turno),
                        new HashSet<>()));
    }

    /** Solicitud de empleador resuelta: al usuario que la envió. */
    public void solicitudResuelta(SolicitudEmpleador solicitud, boolean aprobada) {
        ejecutar("solicitud resuelta", () ->
                avisar(solicitud.getUsuario(), TipoNotificacion.SOLICITUD,
                        aprobada ? "Solicitud aprobada" : "Solicitud rechazada",
                        aprobada
                                ? "Tu solicitud para ser empleador fue aprobada. Ya podés crear sucursales."
                                : "Tu solicitud para ser empleador fue rechazada. Podés enviar una nueva.",
                        TipoDestinoNotificacion.SOLICITUD, solicitud.getId(), null,
                        new HashSet<>()));
    }

    /**
     * Mensaje nuevo: a todos los participantes salvo el autor, sin incluir el contenido.
     * Los destinatarios se resuelven dentro del bloque seguro: si falla la consulta de
     * participantes, el mensaje ya enviado no se ve afectado.
     */
    public void mensajeNuevo(Integer conversacionId, Supplier<List<Usuario>> proveedor) {
        ejecutar("mensaje nuevo", () -> {
            Set<String> avisados = new HashSet<>();
            for (Usuario destinatario : proveedor.get()) {
                avisar(destinatario, TipoNotificacion.MENSAJE,
                        "Nuevo mensaje", "Recibiste un mensaje nuevo en una conversación.",
                        TipoDestinoNotificacion.CONVERSACION, conversacionId, null, avisados);
            }
        });
    }

    private void avisar(Usuario destinatario, TipoNotificacion tipo, String titulo,
                        String mensaje, TipoDestinoNotificacion destinoTipo, Integer destinoId,
                        Integer sucursalId, Set<String> avisados) {
        if (destinatario == null || destinatario.getEmail() == null) {
            return;
        }
        String email = destinatario.getEmail().trim().toLowerCase(Locale.ROOT);
        if (email.isEmpty() || !avisados.add(email)) {
            return;
        }
        try {
            notificacionService.registrar(email, tipo, titulo, mensaje,
                    destinoTipo, destinoId, sucursalId);
        } catch (Exception ex) {
            // No se registran emails ni contenido: la notificación es secundaria.
            log.warn("No se pudo generar la notificación '{}'. La operación original continúa. Causa: {}",
                    titulo, ex.getClass().getSimpleName());
        }
    }

    /** Aísla fallos al extraer datos del evento (relaciones ausentes, etc.). */
    private void ejecutar(String evento, Runnable accion) {
        try {
            accion.run();
        } catch (Exception ex) {
            log.warn("No se pudo generar la notificación del evento '{}'. La operación original continúa. Causa: {}",
                    evento, ex.getClass().getSimpleName());
        }
    }

    private Integer sucursalId(Turno turno) {
        if (turno == null || turno.getServicio() == null
                || turno.getServicio().getSucursal() == null) {
            return null;
        }
        return turno.getServicio().getSucursal().getId();
    }

    private Usuario empleador(Turno turno) {
        if (turno == null || turno.getServicio() == null
                || turno.getServicio().getSucursal() == null) {
            return null;
        }
        return turno.getServicio().getSucursal().getEmpleador();
    }
}
