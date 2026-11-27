package com.gg.turnlook.features.pago;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MockPayoutAdapterTest {

    private MockPayoutAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MockPayoutAdapter();
    }

    @Test
    void pagaYDevuelveEstadoPagado() {
        PayoutResult resultado = adapter.pay(new PayoutCommand(
                new BigDecimal("1500.00"), 7, "pago-1", "idem-1"));

        assertEquals(PayoutStatus.PAID, resultado.status());
        assertEquals("pago-1", resultado.reference());
        assertNotNull(resultado.providerReference());
        assertEquals(PayoutStatus.PAID, adapter.getStatus("pago-1"));
    }

    @Test
    void laMismaClaveDeIdempotenciaNoGeneraOtraOrden() {
        PayoutCommand command = new PayoutCommand(
                new BigDecimal("1500.00"), 7, "pago-2", "idem-repetida");

        PayoutResult primera = adapter.pay(command);
        PayoutResult segunda = adapter.pay(command);

        assertEquals(primera, segunda);
        assertEquals(primera.providerReference(), segunda.providerReference());
    }

    @Test
    void clavesDistintasGeneranReferenciasDeProveedorDistintas() {
        PayoutResult primera = adapter.pay(new PayoutCommand(
                new BigDecimal("100.00"), 7, "pago-a", "idem-a"));
        PayoutResult segunda = adapter.pay(new PayoutCommand(
                new BigDecimal("100.00"), 7, "pago-b", "idem-b"));

        assertNotEquals(primera.providerReference(), segunda.providerReference());
    }

    @Test
    void reembolsaUnaOrdenExistenteYRespetaIdempotencia() {
        adapter.pay(new PayoutCommand(new BigDecimal("1500.00"), 7, "pago-3", "idem-pago-3"));

        RefundCommand command = new RefundCommand("pago-3", new BigDecimal("1500.00"),
                "Turno cancelado", "idem-refund-3");
        PayoutResult primera = adapter.refund(command);
        PayoutResult segunda = adapter.refund(command);

        assertEquals(PayoutStatus.REFUNDED, primera.status());
        assertEquals(primera, segunda);
        assertEquals(PayoutStatus.REFUNDED, adapter.getStatus("pago-3"));
    }

    @Test
    void reembolsoSinOrdenPreviaFalla() {
        PayoutResult resultado = adapter.refund(new RefundCommand("inexistente",
                new BigDecimal("50.00"), "Error", "idem-refund-fail"));

        assertEquals(PayoutStatus.FAILED, resultado.status());
    }

    @Test
    void estadoDeReferenciaDesconocidaEsPendiente() {
        assertEquals(PayoutStatus.PENDING, adapter.getStatus("no-existe"));
    }
}
