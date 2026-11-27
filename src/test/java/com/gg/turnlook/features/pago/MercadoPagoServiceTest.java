package com.gg.turnlook.features.pago;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gg.turnlook.features.payment.PaymentGateway;
import com.gg.turnlook.features.payment.PaymentState;
import com.gg.turnlook.features.payment.records.PagoProveedor;
import com.gg.turnlook.features.servicio.Servicio;
import com.gg.turnlook.features.turno.EstadoTurno;
import com.gg.turnlook.features.turno.Turno;
import com.gg.turnlook.features.turno.TurnoService;
import com.gg.turnlook.features.usuario.Usuario;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MercadoPagoServiceTest {

    private TurnoService turnoService;
    private PaymentGateway gateway;
    private PagoRepository pagoRepository;
    private CobroService cobroService;
    private MercadoPagoService service;

    @BeforeEach
    void setUp() {
        turnoService = mock(TurnoService.class);
        gateway = mock(PaymentGateway.class);
        pagoRepository = mock(PagoRepository.class);
        cobroService = mock(CobroService.class);
        service = new MercadoPagoService(turnoService, gateway, pagoRepository, cobroService);
    }

    @Test
    void pagoAprobadoConfirmaElTurnoYRegistraElAdeudo() {
        Pago interno = pagoDePrueba();
        when(gateway.consultarPago("77")).thenReturn(new PagoProveedor(
                "PREF-77", "50", PaymentState.CAPTURED));
        when(pagoRepository.findByExternalReference("50")).thenReturn(Optional.of(interno));
        when(turnoService.setearPagoAprobado(22)).thenReturn(EstadoTurno.CONFIRMADO);

        assertEquals("Pago aprobado. Turno confirmado", service.procesarWebhook(77L));
        verify(cobroService).confirmarCobro(50, "PREF-77", PaymentState.CAPTURED);
        verify(turnoService).setearPagoAprobado(22);
    }

    @Test
    void pagoPendienteNoSeMarcaComoRechazado() {
        Pago interno = pagoDePrueba();
        when(gateway.consultarPago("77")).thenReturn(new PagoProveedor(
                "PREF-77", "50", PaymentState.AUTHORIZED));
        when(pagoRepository.findByExternalReference("50")).thenReturn(Optional.of(interno));
        when(turnoService.verificarTurnoParaWebhook(22)).thenReturn(EstadoTurno.PENDIENTE);

        assertEquals("Pago pendiente de resolución", service.procesarWebhook(77L));
        verify(cobroService, never()).confirmarCobro(any(), any(), any());
        verify(turnoService, never()).setearPagoRechazado(22);
    }

    @Test
    void pagoRechazadoMarcaElTurno() {
        Pago interno = pagoDePrueba();
        when(gateway.consultarPago("77")).thenReturn(new PagoProveedor(
                "PREF-77", "50", PaymentState.FAILED));
        when(pagoRepository.findByExternalReference("50")).thenReturn(Optional.of(interno));
        when(turnoService.setearPagoRechazado(22)).thenReturn(EstadoTurno.PAGO_RECHAZADO);

        assertEquals("Pago rechazado", service.procesarWebhook(77L));
        verify(cobroService).confirmarCobro(50, "PREF-77", PaymentState.FAILED);
        verify(turnoService).setearPagoRechazado(22);
    }

    @Test
    void referenciaAusenteNoModificaElTurno() {
        when(gateway.consultarPago("77")).thenReturn(new PagoProveedor(
                "PREF-77", null, PaymentState.CAPTURED));

        assertThrows(IllegalArgumentException.class, () -> service.procesarWebhook(77L));
        verify(turnoService, never()).setearPagoAprobado(22);
    }

    @Test
    void pagarDelegaEnCobroServiceYDevuelveTextoPlano() {
        when(cobroService.iniciarCobro(22, "cliente@example.com")).thenReturn("https://init");

        assertEquals("https://init", service.pagar(22, "cliente@example.com"));
        verify(cobroService).iniciarCobro(22, "cliente@example.com");
    }

    private Pago pagoDePrueba() {
        Usuario cliente = new Usuario("Ana", "Paz", "cliente@example.com", "hash");
        Servicio servicio = new Servicio("Corte", "Corte de pelo", 30,
                new BigDecimal("1500.00"), null);
        Turno turno = new Turno();
        turno.setId(22);
        turno.setCliente(cliente);
        turno.setServicio(servicio);
        turno.setEstado(EstadoTurno.PENDIENTE);
        Pago pago = new Pago();
        pago.setId(50);
        pago.setTurno(turno);
        pago.setExternalReference("50");
        return pago;
    }
}
