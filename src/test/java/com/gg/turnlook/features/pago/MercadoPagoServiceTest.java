package com.gg.turnlook.features.pago;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gg.turnlook.common.exception.ForbiddenException;
import com.gg.turnlook.features.servicio.Servicio;
import com.gg.turnlook.features.turno.EstadoTurno;
import com.gg.turnlook.features.turno.Turno;
import com.gg.turnlook.features.turno.TurnoService;
import com.gg.turnlook.features.usuario.Usuario;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MercadoPagoServiceTest {

    private TurnoService turnoService;
    private PaymentGateway gateway;
    private MercadoPagoService service;

    @BeforeEach
    void setUp() {
        turnoService = mock(TurnoService.class);
        gateway = mock(PaymentGateway.class);
        service = new MercadoPagoService(turnoService, gateway);
    }

    @Test
    void pagoAprobadoConfirmaElTurno() {
        when(gateway.consultarPago("77")).thenReturn(new PagoProveedor("77", "123", PaymentState.CAPTURED));
        when(turnoService.setearPagoAprobado(123)).thenReturn(EstadoTurno.CONFIRMADO);

        assertEquals("Pago aprobado. Turno confirmado", service.procesarWebhook(77L));
        verify(turnoService).setearPagoAprobado(123);
        verify(turnoService, never()).setearPagoRechazado(123);
    }

    @Test
    void pagoPendienteNoSeMarcaComoRechazado() {
        when(gateway.consultarPago("77")).thenReturn(new PagoProveedor("77", "123", PaymentState.AUTHORIZED));
        when(turnoService.verificarTurnoParaWebhook(123)).thenReturn(EstadoTurno.PENDIENTE);

        assertEquals("Pago pendiente de resolución", service.procesarWebhook(77L));
        verify(turnoService).verificarTurnoParaWebhook(123);
        verify(turnoService, never()).setearPagoRechazado(123);
    }

    @Test
    void pagoRechazadoMarcaElTurno() {
        when(gateway.consultarPago("77")).thenReturn(new PagoProveedor("77", "123", PaymentState.FAILED));
        when(turnoService.setearPagoRechazado(123)).thenReturn(EstadoTurno.PAGO_RECHAZADO);

        assertEquals("Pago rechazado", service.procesarWebhook(77L));
        verify(turnoService).setearPagoRechazado(123);
    }

    @Test
    void referenciaAusenteNoModificaElTurno() {
        when(gateway.consultarPago("77")).thenReturn(new PagoProveedor("77", null, PaymentState.CAPTURED));

        assertThrows(IllegalArgumentException.class, () -> service.procesarWebhook(77L));
        verify(turnoService, never()).setearPagoAprobado(123);
    }

    @Test
    void creaCobroConReferenciaDelTurnoEIdempotencia() {
        when(turnoService.listarTurnoPorId(5)).thenReturn(turnoDePrueba());
        when(gateway.crearCobro(any())).thenReturn(new CobroCreado(
                "PREF-5", "https://init", "5", PaymentState.CREATED));

        assertEquals("https://init", service.pagar(5, "cliente@example.com"));

        ArgumentCaptor<CrearCobroCommand> captor = ArgumentCaptor.forClass(CrearCobroCommand.class);
        verify(gateway).crearCobro(captor.capture());
        assertEquals(5, captor.getValue().pagoId());
        assertEquals("pago-turno-5", captor.getValue().idempotencyKey());
        assertEquals(new BigDecimal("1500.00"), captor.getValue().monto());
    }

    @Test
    void clienteDistintoNoPuedePagar() {
        when(turnoService.listarTurnoPorId(5)).thenReturn(turnoDePrueba());

        assertThrows(ForbiddenException.class, () -> service.pagar(5, "otro@example.com"));
        verify(gateway, never()).crearCobro(any());
    }

    private Turno turnoDePrueba() {
        Usuario cliente = new Usuario("Ana", "Paz", "cliente@example.com", "hash");
        Servicio servicio = new Servicio("Corte", "Corte de pelo", 30,
                new BigDecimal("1500.00"), null);
        Turno turno = new Turno();
        turno.setId(5);
        turno.setCliente(cliente);
        turno.setServicio(servicio);
        turno.setEstado(EstadoTurno.PENDIENTE);
        return turno;
    }
}
