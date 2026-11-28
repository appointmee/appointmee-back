package com.gg.turnlook.features.mercadopago;

import com.gg.turnlook.features.mercadopago.api.MercadoPagoApi;
import com.gg.turnlook.features.mercadopago.api.PagoApiResponse;
import com.gg.turnlook.features.mercadopago.api.PreferenciaRequest;
import com.gg.turnlook.features.mercadopago.api.PreferenciaResponse;
import com.gg.turnlook.features.payment.PaymentProviderException;
import com.gg.turnlook.features.payment.PaymentState;
import com.gg.turnlook.features.payment.comandos.CrearCobroCommand;
import com.gg.turnlook.features.payment.records.CobroCreado;
import com.gg.turnlook.features.payment.records.PagoProveedor;
import com.gg.turnlook.features.servicio.Servicio;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MercadoPagoSandboxAdapterTest {

    private MercadoPagoApi api;
    private MercadoPagoProperties properties;
    private MercadoPagoSandboxAdapter adapter;

    @BeforeEach
    void setUp() {
        api = mock(MercadoPagoApi.class);
        properties = new MercadoPagoProperties();
        properties.setAccessToken("TEST-token");
        properties.setNotificationUrl("http://localhost:8080/pagos/webhook");
        adapter = new MercadoPagoSandboxAdapter(api, properties);
    }

    @Test
    void creaCobroConReferenciaDelPagoYDevuelveInitPoint() {
        when(api.crearPreferencia(any(), eq("TEST-token"), eq("idem-1")))
                .thenReturn(new PreferenciaResponse("PREF-1",
                        "https://sandbox.mercadopago.com/checkout/init", "pending"));

        CobroCreado cobro = adapter.crearCobro(new CrearCobroCommand(
                77, new BigDecimal("1500.00"), "ARS", "Corte de pelo",
                "cliente@example.com", "idem-1"));

        assertEquals("77", cobro.externalReference());
        assertEquals("https://sandbox.mercadopago.com/checkout/init", cobro.initPoint());
        assertEquals(PaymentState.AUTHORIZED, cobro.estado());
    }

    @Test
    void enviaTokenDeAplicacionNotificationUrlYIdempotencia() {
        when(api.crearPreferencia(any(), any(), any()))
                .thenReturn(new PreferenciaResponse("PREF-2", "https://init", "pending"));

        adapter.crearCobro(new CrearCobroCommand(
                78, new BigDecimal("999.00"), "ARS", "Servicio", "p@example.com", "idem-2"));

        ArgumentCaptor<PreferenciaRequest> captor = ArgumentCaptor.forClass(PreferenciaRequest.class);
        verify(api).crearPreferencia(captor.capture(), eq("TEST-token"), eq("idem-2"));
        PreferenciaRequest request = captor.getValue();
        assertEquals("http://localhost:8080/pagos/webhook", request.notificationUrl());
        assertEquals("78", request.externalReference());
        assertEquals("ARS", request.items().get(0).currencyId());
        assertEquals(new BigDecimal("999.00"), request.items().get(0).unitPrice());
    }

    @Test
    void mapeaEstadosDelProveedorDeFormaExplicita() {
        assertEquals(EstadoPagoProveedor.APPROVED, EstadoPagoProveedor.desdeProveedor("approved", "accredited"));
        assertEquals(EstadoPagoProveedor.REJECTED, EstadoPagoProveedor.desdeProveedor("rejected", "cc_rejected"));
        assertEquals(EstadoPagoProveedor.PENDING, EstadoPagoProveedor.desdeProveedor("pending", "pending_contingency"));
        assertEquals(EstadoPagoProveedor.CHARGED_BACK, EstadoPagoProveedor.desdeProveedor("charged_back", null));
        assertEquals(EstadoPagoProveedor.UNKNOWN, EstadoPagoProveedor.desdeProveedor("otro", null));
        assertEquals(PaymentState.CAPTURED, EstadoPagoProveedor.APPROVED.aPaymentState());
        assertEquals(PaymentState.FAILED, EstadoPagoProveedor.REJECTED.aPaymentState());
        assertEquals(PaymentState.AUTHORIZED, EstadoPagoProveedor.PENDING.aPaymentState());
        assertEquals(PaymentState.CHARGED_BACK, EstadoPagoProveedor.CHARGED_BACK.aPaymentState());
    }

    @Test
    void traduceElEstadoConsultadoAlDominio() {
        when(api.obtenerPago("PAGO-1", "TEST-token"))
                .thenReturn(new PagoApiResponse("PAGO-1", "approved", "accredited", "77"));

        PagoProveedor pago = adapter.consultarPago("PAGO-1");
        assertEquals(PaymentState.CAPTURED, pago.estado());
        assertEquals("77", pago.externalReference());
    }

    @Test
    void noFiltraExcepcionesDelProveedorHaciaElDominio() {
        when(api.crearPreferencia(any(), any(), any()))
                .thenThrow(new IllegalStateException("fallo de red del proveedor"));

        PaymentProviderException error = assertThrows(PaymentProviderException.class,
                () -> adapter.crearCobro(new CrearCobroCommand(
                        79, new BigDecimal("10.00"), "ARS", "Servicio", "p@example.com", "idem-3")));
        assertEquals("fallo de red del proveedor", error.getCause().getMessage());
    }

    @Test
    void consultarEstadoConFalloDelProveedorLanzaErrorDeDominio() {
        when(api.obtenerPago("PAGO-2", "TEST-token"))
                .thenThrow(new IllegalStateException("timeout"));

        assertThrows(PaymentProviderException.class, () -> adapter.consultarPago("PAGO-2"));
    }
}
