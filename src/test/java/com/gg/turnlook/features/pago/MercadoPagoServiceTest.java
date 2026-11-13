package com.gg.turnlook.features.pago;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gg.turnlook.features.turno.EstadoTurno;
import com.gg.turnlook.features.turno.TurnoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MercadoPagoServiceTest {

    private TurnoService turnoService;
    private MercadoPagoGateway gateway;
    private MercadoPagoService service;

    @BeforeEach
    void setUp() {
        turnoService = mock(TurnoService.class);
        gateway = mock(MercadoPagoGateway.class);
        service = new MercadoPagoService(turnoService, gateway);
    }

    @Test
    void pagoAprobadoConfirmaElTurno() throws Exception {
        when(gateway.obtenerPago(77L)).thenReturn(new PagoMercadoPago("approved", "123"));
        when(turnoService.setearPagoAprobado(123)).thenReturn(EstadoTurno.CONFIRMADO);

        assertEquals("Pago aprobado. Turno confirmado", service.procesarWebhook(77L));
        verify(turnoService).setearPagoAprobado(123);
        verify(turnoService, never()).setearPagoRechazado(123);
    }

    @Test
    void pagoPendienteNoSeMarcaComoRechazado() throws Exception {
        when(gateway.obtenerPago(77L)).thenReturn(new PagoMercadoPago("in_process", "123"));
        when(turnoService.verificarTurnoParaWebhook(123)).thenReturn(EstadoTurno.PENDIENTE);

        assertEquals("Pago pendiente de resolución", service.procesarWebhook(77L));
        verify(turnoService).verificarTurnoParaWebhook(123);
        verify(turnoService, never()).setearPagoRechazado(123);
    }

    @Test
    void pagoRechazadoMarcaElTurno() throws Exception {
        when(gateway.obtenerPago(77L)).thenReturn(new PagoMercadoPago("rejected", "123"));
        when(turnoService.setearPagoRechazado(123)).thenReturn(EstadoTurno.PAGO_RECHAZADO);

        assertEquals("Pago rechazado", service.procesarWebhook(77L));
        verify(turnoService).setearPagoRechazado(123);
    }

    @Test
    void referenciaAusenteNoModificaElTurno() throws Exception {
        when(gateway.obtenerPago(77L)).thenReturn(new PagoMercadoPago("approved", null));

        assertThrows(IllegalArgumentException.class, () -> service.procesarWebhook(77L));
        verify(turnoService, never()).setearPagoAprobado(123);
    }
}
