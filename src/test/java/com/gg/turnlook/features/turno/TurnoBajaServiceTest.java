package com.gg.turnlook.features.turno;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gg.turnlook.features.notificacion.NotificacionEventosService;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** SCRUM-365: reglas de cancelación por baja lógica y desvinculación. */
class TurnoBajaServiceTest {

    private TurnoRepository turnoRepo;
    private NotificacionEventosService eventos;
    private TurnoBajaService service;

    @BeforeEach
    void setUp() {
        turnoRepo = mock(TurnoRepository.class);
        eventos = mock(NotificacionEventosService.class);
        service = new TurnoBajaService(turnoRepo, eventos);
    }

    @Test
    void bajaDeEmpleadoCancelaPendienteYConfirmado() {
        Turno pendiente = turno(1, EstadoTurno.PENDIENTE);
        Turno confirmado = turno(2, EstadoTurno.CONFIRMADO);
        when(turnoRepo.findCancelablesPorEmpleado(eq(9), anyCollection()))
                .thenReturn(List.of(pendiente, confirmado));

        int cancelados = service.cancelarPorBajaEmpleado(9);

        assertEquals(2, cancelados);
        assertEquals(EstadoTurno.CANCELADO, pendiente.getEstado());
        assertEquals(EstadoTurno.CANCELADO, confirmado.getEstado());
        assertNotNull(pendiente.getFechaModificacionEstado());
        verify(turnoRepo, times(2)).save(any(Turno.class));
        verify(eventos, times(2)).turnoCancelado(any(Turno.class));
        verify(turnoRepo).findCancelablesPorEmpleado(eq(9),
                argThat((Collection<EstadoTurno> estados) ->
                        estados.size() == 2
                                && estados.contains(EstadoTurno.PENDIENTE)
                                && estados.contains(EstadoTurno.CONFIRMADO)));
    }

    @Test
    void bajaDeSucursalCancelaTodosSusTurnosAbiertos() {
        Turno pendiente = turno(1, EstadoTurno.PENDIENTE);
        Turno confirmado = turno(2, EstadoTurno.CONFIRMADO);
        when(turnoRepo.findCancelablesPorSucursal(eq(4), anyCollection()))
                .thenReturn(List.of(pendiente, confirmado));

        int cancelados = service.cancelarPorBajaSucursal(4);

        assertEquals(2, cancelados);
        assertEquals(EstadoTurno.CANCELADO, pendiente.getEstado());
        assertEquals(EstadoTurno.CANCELADO, confirmado.getEstado());
        verify(turnoRepo).findCancelablesPorSucursal(eq(4), anyCollection());
        verify(turnoRepo, never()).findCancelablesPorEmpleado(eq(9), anyCollection());
    }

    @Test
    void desvinculacionSoloConsultaLosTurnosDeEsaRelacion() {
        Turno deEstaSucursal = turno(1, EstadoTurno.CONFIRMADO);
        Turno deOtraSucursal = turno(2, EstadoTurno.CONFIRMADO);
        when(turnoRepo.findCancelablesPorEmpleadoYSucursal(eq(9), eq(4), anyCollection()))
                .thenReturn(List.of(deEstaSucursal));

        int cancelados = service.cancelarPorDesvinculacion(9, 4);

        assertEquals(1, cancelados);
        assertEquals(EstadoTurno.CANCELADO, deEstaSucursal.getEstado());
        assertEquals(EstadoTurno.CONFIRMADO, deOtraSucursal.getEstado());
        verify(turnoRepo).findCancelablesPorEmpleadoYSucursal(eq(9), eq(4), anyCollection());
        verify(turnoRepo, never()).findCancelablesPorEmpleado(eq(9), anyCollection());
        verify(turnoRepo, never()).findCancelablesPorSucursal(eq(4), anyCollection());
    }

    @Test
    void estadosCerradosSeConservanIntactos() {
        Turno cancelado = turno(1, EstadoTurno.CANCELADO);
        Turno realizado = turno(2, EstadoTurno.REALIZADO);
        Turno rechazado = turno(3, EstadoTurno.PAGO_RECHAZADO);
        when(turnoRepo.findCancelablesPorEmpleado(eq(9), anyCollection()))
                .thenReturn(List.of(cancelado, realizado, rechazado));
        when(turnoRepo.findCancelablesPorSucursal(eq(4), anyCollection()))
                .thenReturn(List.of(cancelado, realizado, rechazado));
        when(turnoRepo.findCancelablesPorEmpleadoYSucursal(eq(9), eq(4), anyCollection()))
                .thenReturn(List.of(cancelado, realizado, rechazado));

        assertEquals(0, service.cancelarPorBajaEmpleado(9));
        assertEquals(0, service.cancelarPorBajaSucursal(4));
        assertEquals(0, service.cancelarPorDesvinculacion(9, 4));

        assertEquals(EstadoTurno.CANCELADO, cancelado.getEstado());
        assertEquals(EstadoTurno.REALIZADO, realizado.getEstado());
        assertEquals(EstadoTurno.PAGO_RECHAZADO, rechazado.getEstado());
        assertNull(realizado.getFechaModificacionEstado());
        verify(turnoRepo, never()).save(any(Turno.class));
        verify(eventos, never()).turnoCancelado(any(Turno.class));
    }

    @Test
    void laCancelacionNoGeneraMovimientosFinancieros() {
        when(turnoRepo.findCancelablesPorSucursal(eq(4), anyCollection()))
                .thenReturn(List.of(turno(1, EstadoTurno.CONFIRMADO)));

        service.cancelarPorBajaSucursal(4);

        verify(eventos).turnoCancelado(any(Turno.class));
        verify(eventos, never()).pagoAprobado(any());
        verify(eventos, never()).pagoRechazado(any());
        verify(eventos, never()).turnoReservado(any(Turno.class));
        verify(eventos, never()).turnoFinalizado(any(Turno.class));
    }

    @Test
    void unFalloAlNotificarNoRevierteLaCancelacion() {
        Turno turno = turno(1, EstadoTurno.PENDIENTE);
        when(turnoRepo.findCancelablesPorEmpleado(eq(9), anyCollection()))
                .thenReturn(List.of(turno));
        doThrow(new RuntimeException("WS caído")).when(eventos).turnoCancelado(turno);

        int cancelados = service.cancelarPorBajaEmpleado(9);

        assertEquals(1, cancelados);
        assertEquals(EstadoTurno.CANCELADO, turno.getEstado());
        verify(turnoRepo).save(turno);
    }

    @Test
    void unFalloAlPersistirSePropagaParaQueLaTransaccionSeRevierte() {
        Turno turno = turno(1, EstadoTurno.PENDIENTE);
        when(turnoRepo.findCancelablesPorEmpleado(eq(9), anyCollection()))
                .thenReturn(List.of(turno, turno(2, EstadoTurno.CONFIRMADO)));
        doThrow(new RuntimeException("DB caída")).when(turnoRepo).save(turno);

        assertThrows(RuntimeException.class, () -> service.cancelarPorBajaEmpleado(9));
        verify(eventos, never()).turnoCancelado(any(Turno.class));
    }

    private Turno turno(int id, EstadoTurno estado) {
        Turno turno = new Turno();
        turno.setId(id);
        turno.setEstado(estado);
        return turno;
    }
}
