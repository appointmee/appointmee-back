package com.gg.turnlook.features.pago;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gg.turnlook.common.exception.BadRequestException;
import com.gg.turnlook.common.exception.ConflictException;
import com.gg.turnlook.common.exception.ForbiddenException;
import com.gg.turnlook.features.ledger.FinancialOperationService;
import com.gg.turnlook.features.ledger.FinancialOperationType;
import com.gg.turnlook.features.payment.PaymentGateway;
import com.gg.turnlook.features.payment.PaymentState;
import com.gg.turnlook.features.payment.SettlementState;
import com.gg.turnlook.features.payment.comandos.CrearCobroCommand;
import com.gg.turnlook.features.payment.records.CobroCreado;
import com.gg.turnlook.features.servicio.Servicio;
import com.gg.turnlook.features.sucursal.Sucursal;
import com.gg.turnlook.features.turno.EstadoTurno;
import com.gg.turnlook.features.turno.Turno;
import com.gg.turnlook.features.turno.TurnoService;
import com.gg.turnlook.features.usuario.Usuario;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CobroServiceTest {

    private PagoRepository pagoRepository;
    private TurnoService turnoService;
    private PaymentGateway paymentGateway;
    private FinancialOperationService ledger;
    private CobroService service;

    private Turno turno;
    private Sucursal sucursal;

    @BeforeEach
    void setUp() {
        pagoRepository = mock(PagoRepository.class);
        turnoService = mock(TurnoService.class);
        paymentGateway = mock(PaymentGateway.class);
        ledger = mock(FinancialOperationService.class);
        service = new CobroService(pagoRepository, turnoService, paymentGateway, ledger);

        sucursal = new Sucursal();
        sucursal.setId(7);
        sucursal.setNombre("Centro");
        Usuario cliente = new Usuario("Ana", "Paz", "cliente@example.com", "hash");
        Servicio servicio = new Servicio("Corte", "Corte de pelo", 30,
                new BigDecimal("1500.00"), sucursal);
        turno = new Turno();
        turno.setId(22);
        turno.setCliente(cliente);
        turno.setServicio(servicio);
        turno.setEstado(EstadoTurno.PENDIENTE);

        when(pagoRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Pago pago = invocation.getArgument(0);
            if (pago.getId() == null) {
                pago.setId(50);
            }
            return pago;
        });
    }

    @Test
    void turnoNoPendienteNoCreaPago() {
        turno.setEstado(EstadoTurno.CONFIRMADO);
        when(turnoService.listarTurnoPorId(22)).thenReturn(turno);

        assertThrows(ConflictException.class, () -> service.iniciarCobro(22, "cliente@example.com"));
        verify(paymentGateway, never()).crearCobro(any());
    }

    @Test
    void clienteDistintoNoCreaPago() {
        when(turnoService.listarTurnoPorId(22)).thenReturn(turno);

        assertThrows(ForbiddenException.class, () -> service.iniciarCobro(22, "otro@example.com"));
        verify(paymentGateway, never()).crearCobro(any());
    }

    @Test
    void importeInvalidoNoCreaPago() {
        turno.getServicio().setPrecio(BigDecimal.ZERO);
        when(turnoService.listarTurnoPorId(22)).thenReturn(turno);

        assertThrows(BadRequestException.class, () -> service.iniciarCobro(22, "cliente@example.com"));
        verify(paymentGateway, never()).crearCobro(any());
    }

    @Test
    void creaPagoConImporteDelServicioMonedaArsYGuardaInitPoint() {
        when(turnoService.listarTurnoPorId(22)).thenReturn(turno);
        when(paymentGateway.crearCobro(any())).thenReturn(new CobroCreado(
                "PREF-50", "https://init", "50", PaymentState.CREATED));

        String initPoint = service.iniciarCobro(22, "cliente@example.com");

        assertEquals("https://init", initPoint);
        ArgumentCaptor<Pago> captor = ArgumentCaptor.forClass(Pago.class);
        verify(pagoRepository, org.mockito.Mockito.times(2)).saveAndFlush(captor.capture());
        Pago guardado = captor.getValue();
        assertEquals(new BigDecimal("1500.00"), guardado.getAmount());
        assertEquals("ARS", guardado.getCurrency());
        assertEquals("50", guardado.getExternalReference());
        assertEquals("pago-turno-22", guardado.getIdempotencyKey());
        assertEquals(7, guardado.getSucursal().getId());
        assertEquals("https://init", guardado.getCheckoutUrl());
        verify(ledger).registrar(any(), eq(FinancialOperationType.CREATED), any(), any(), any(), any());
    }

    @Test
    void dobleClicConMismaIdempotenciaNoCreaOtroPago() {
        Pago previo = new Pago();
        previo.setCheckoutUrl("https://init-anterior");
        when(pagoRepository.findByIdempotencyKey("pago-turno-22")).thenReturn(Optional.of(previo));

        assertEquals("https://init-anterior", service.iniciarCobro(22, "cliente@example.com"));
        verify(pagoRepository, never()).saveAndFlush(any());
        verify(paymentGateway, never()).crearCobro(any());
    }

    @Test
    void confirmarCobroRegistraAdeudoALaSucursal() {
        Pago pago = pagoDePrueba();
        when(pagoRepository.findById(50)).thenReturn(Optional.of(pago));

        service.confirmarCobro(50, "PREF-50", PaymentState.CAPTURED);

        verify(ledger).cambiarEstado(pago, PaymentState.CAPTURED, SettlementState.HELD_POLICY,
                FinancialOperationType.APPROVED, "PREF-50", "webhook");
        verify(ledger).registrar(pago, FinancialOperationType.DEBIT_REGISTERED,
                PaymentState.CAPTURED + "/" + SettlementState.HELD_POLICY,
                "ADEUDO_SUCURSAL_7", "PREF-50", "sistema");
    }

    @Test
    void confirmarCobroRepetidoNoRegistraOtraVez() {
        Pago pago = pagoDePrueba();
        pago.setPaymentState(PaymentState.CAPTURED);
        pago.setSettlementState(SettlementState.HELD_POLICY);
        when(pagoRepository.findById(50)).thenReturn(Optional.of(pago));

        service.confirmarCobro(50, "PREF-50", PaymentState.CAPTURED);

        verify(ledger, never()).cambiarEstado(any(), any(), any(), any(), any(), any());
        verify(ledger, never()).registrar(any(), any(), any(), any(), any(), any());
    }

    @Test
    void confirmarCobroFallidoRegistraEstado() {
        Pago pago = pagoDePrueba();
        when(pagoRepository.findById(50)).thenReturn(Optional.of(pago));

        service.confirmarCobro(50, "PREF-50", PaymentState.FAILED);

        verify(ledger).cambiarEstado(pago, PaymentState.FAILED, SettlementState.NOT_DUE,
                FinancialOperationType.PAYMENT_FAILED, "PREF-50", "webhook");
    }

    private Pago pagoDePrueba() {
        Pago pago = new Pago();
        pago.setId(50);
        pago.setTurno(turno);
        pago.setSucursal(sucursal);
        pago.setAmount(new BigDecimal("1500.00"));
        pago.setCurrency("ARS");
        pago.setPaymentState(PaymentState.CREATED);
        pago.setSettlementState(SettlementState.NOT_DUE);
        return pago;
    }
}
