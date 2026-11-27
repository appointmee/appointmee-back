package com.gg.turnlook.features.turno;

import com.gg.turnlook.features.notificacion.NotificacionEventosService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cancelaciones de turnos derivadas de bajas lógicas y desvinculaciones.
 *
 * <p>Sólo se cancelan los estados que todavía pueden avanzar (PENDIENTE y
 * CONFIRMADO). CANCELADO, REALIZADO y PAGO_RECHAZADO forman parte del
 * histórico y se conservan intactos. La regla se aplica también aquí, además
 * de en las consultas, para que ningún llamador pueda alterar un turno ya
 * cerrado. Este servicio no interactúa con pagos: la cancelación no genera
 * reembolsos ni movimientos financieros.</p>
 */
@Service
@RequiredArgsConstructor
public class TurnoBajaService {

    static final List<EstadoTurno> ESTADOS_CANCELABLES =
            List.of(EstadoTurno.PENDIENTE, EstadoTurno.CONFIRMADO);

    private static final Logger log = LoggerFactory.getLogger(TurnoBajaService.class);

    private final TurnoRepository turnoRepo;
    private final NotificacionEventosService notificacionEventosService;

    /** Baja lógica global del empleado: alcanza todos sus turnos cancelables. */
    @Transactional
    public int cancelarPorBajaEmpleado(Integer empleadoId) {
        return cancelar(turnoRepo.findCancelablesPorEmpleado(
                empleadoId, ESTADOS_CANCELABLES));
    }

    /** Baja lógica de la sucursal: alcanza todos sus turnos cancelables. */
    @Transactional
    public int cancelarPorBajaSucursal(Integer sucursalId) {
        return cancelar(turnoRepo.findCancelablesPorSucursal(
                sucursalId, ESTADOS_CANCELABLES));
    }

    /** Desvinculación de una relación: sólo los turnos de esa empleado-sucursal. */
    @Transactional
    public int cancelarPorDesvinculacion(Integer empleadoId, Integer sucursalId) {
        return cancelar(turnoRepo.findCancelablesPorEmpleadoYSucursal(
                empleadoId, sucursalId, ESTADOS_CANCELABLES));
    }

    private int cancelar(List<Turno> turnos) {
        if (turnos == null || turnos.isEmpty()) return 0;

        LocalDateTime ahora = LocalDateTime.now();
        int cancelados = 0;
        for (Turno turno : turnos) {
            if (turno == null || !ESTADOS_CANCELABLES.contains(turno.getEstado())) {
                continue;
            }
            turno.setEstado(EstadoTurno.CANCELADO);
            turno.setFechaModificacionEstado(ahora);
            turnoRepo.save(turno);
            notificar(turno);
            cancelados++;
        }
        return cancelados;
    }

    /** Notificar es secundario: un fallo nunca debe revertir la cancelación. */
    private void notificar(Turno turno) {
        try {
            notificacionEventosService.turnoCancelado(turno);
        } catch (Exception ex) {
            log.warn("No se pudo notificar la cancelación del turno {}. La baja continúa. Causa: {}",
                    turno.getId(), ex.getClass().getSimpleName());
        }
    }
}
