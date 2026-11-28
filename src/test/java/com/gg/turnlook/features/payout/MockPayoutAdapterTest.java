package com.gg.turnlook.features.payout;

import com.gg.turnlook.features.payment.PayoutProvider;
import com.gg.turnlook.features.payment.comandos.PayoutCommand;
import com.gg.turnlook.features.payment.comandos.RefundCommand;
import com.gg.turnlook.features.payment.records.PayoutResult;
import com.gg.turnlook.features.turno.Turno;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MockPayoutAdapterTest {

    private MockPayoutAdapter adapter;
    private InMemoryPayoutStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryPayoutStore();
        adapter = new MockPayoutAdapter(store);
    }

    @Test
    void pagaYPersisteElResultado() {
        PayoutResult resultado = adapter.pay(new PayoutCommand(
                new BigDecimal("1500.00"), 7, "pago-1", "idem-1"));

        assertEquals(PayoutStatus.PAID, resultado.status());
        assertEquals("pago-1", resultado.reference());
        assertNotNull(resultado.providerReference());
        assertEquals(PayoutStatus.PAID, adapter.getStatus("pago-1"));
        assertEquals(PayoutStatus.PAID, store.findByIdempotencyKey("idem-1").orElseThrow().status());
        assertEquals(7, store.findByIdempotencyKey("idem-1").orElseThrow().sucursalId());
    }

    @Test
    void laMismaClaveDeIdempotenciaNoGeneraOtraOrden() {
        PayoutCommand command = new PayoutCommand(
                new BigDecimal("1500.00"), 7, "pago-2", "idem-repetida");

        PayoutResult primera = adapter.pay(command);
        PayoutResult segunda = adapter.pay(command);

        assertEquals(primera, segunda);
        assertEquals(primera.providerReference(), segunda.providerReference());
        assertEquals(1, store.tamanio());
    }

    @Test
    void clavesDistintasGeneranReferenciasDeProveedorDistintas() {
        PayoutResult primera = adapter.pay(new PayoutCommand(
                new BigDecimal("100.00"), 7, "pago-a", "idem-a"));
        PayoutResult segunda = adapter.pay(new PayoutCommand(
                new BigDecimal("100.00"), 7, "pago-b", "idem-b"));

        assertNotEquals(primera.providerReference(), segunda.providerReference());
        assertEquals(2, store.tamanio());
    }

    @Test
    void reembolsaUnaOrdenPersistidaYRespetaIdempotencia() {
        adapter.pay(new PayoutCommand(new BigDecimal("1500.00"), 7, "pago-3", "idem-pago-3"));

        RefundCommand command = new RefundCommand("pago-3", new BigDecimal("1500.00"),
                "Turno cancelado", "idem-refund-3");
        PayoutResult primera = adapter.refund(command);
        PayoutResult segunda = adapter.refund(command);

        assertEquals(PayoutStatus.REFUNDED, primera.status());
        assertEquals(primera, segunda);
        assertEquals(PayoutStatus.REFUNDED, adapter.getStatus("pago-3"));
        assertEquals(2, store.tamanio());
    }

    @Test
    void reembolsoSinOrdenPreviaFalla() {
        PayoutResult resultado = adapter.refund(new RefundCommand("inexistente",
                new BigDecimal("50.00"), "Error", "idem-refund-fail"));

        assertEquals(PayoutStatus.FAILED, resultado.status());
        assertEquals(0, store.tamanio());
    }

    @Test
    void exponeLasFirmasDirectasDelContrato() {
        PayoutProvider proveedor = adapter;

        PayoutResult pago = proveedor.pay(new BigDecimal("250.00"), 9, "pago-directo", "idem-directo");
        assertEquals(PayoutStatus.PAID, pago.status());

        PayoutResult reembolso = proveedor.refund("pago-directo", new BigDecimal("250.00"));
        assertEquals(PayoutStatus.REFUNDED, reembolso.status());
        assertEquals(PayoutStatus.REFUNDED, proveedor.getStatus("pago-directo"));
    }

    @Test
    void estadoDeReferenciaDesconocidaEsPendiente() {
        assertEquals(PayoutStatus.PENDING, adapter.getStatus("no-existe"));
    }

    /** Store en memoria para tests unitarios; en producción usa JpaPayoutStore. */
    private static class InMemoryPayoutStore implements PayoutStore {

        private final Map<String, PayoutRecord> porIdempotencia = new LinkedHashMap<>();
        private final Map<String, PayoutRecord> porReferencia = new LinkedHashMap<>();

        @Override
        public Optional<PayoutRecord> findByIdempotencyKey(String key) {
            return Optional.ofNullable(porIdempotencia.get(key));
        }

        @Override
        public Optional<PayoutRecord> findByReference(String reference) {
            return Optional.ofNullable(porReferencia.get(reference));
        }

        @Override
        public PayoutRecord save(PayoutRecord record) {
            porIdempotencia.put(record.idempotencyKey(), record);
            porReferencia.put(record.reference(), record);
            return record;
        }

        int tamanio() {
            return porIdempotencia.size();
        }
    }
}
