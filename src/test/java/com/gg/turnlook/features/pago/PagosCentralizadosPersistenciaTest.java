package com.gg.turnlook.features.pago;

import com.gg.turnlook.features.ledger.FinancialOperation;
import com.gg.turnlook.features.ledger.FinancialOperationRepository;
import com.gg.turnlook.features.ledger.FinancialOperationService;
import com.gg.turnlook.features.ledger.FinancialOperationType;
import com.gg.turnlook.features.payment.PaymentState;
import com.gg.turnlook.features.payment.SettlementState;
import com.gg.turnlook.features.payment.comandos.PayoutCommand;
import com.gg.turnlook.features.payment.records.PayoutResult;
import com.gg.turnlook.features.payout.JpaPayoutStore;
import com.gg.turnlook.features.payout.MockPayoutAdapter;
import com.gg.turnlook.features.payout.PaymentRelease;
import com.gg.turnlook.features.payout.PaymentReleaseRepository;
import com.gg.turnlook.features.payout.PayoutOperationRepository;
import com.gg.turnlook.features.payout.PayoutStatus;
import com.gg.turnlook.features.servicio.Servicio;
import com.gg.turnlook.features.servicio.ServicioRepository;
import com.gg.turnlook.features.sucursal.Sucursal;
import com.gg.turnlook.features.sucursal.SucursalRepository;
import com.gg.turnlook.features.turno.Turno;
import com.gg.turnlook.features.turno.TurnoRepository;
import com.gg.turnlook.features.usuario.Usuario;
import com.gg.turnlook.features.usuario.UsuarioRepository;
import com.gg.turnlook.features.webhook.PaymentWebhookEvent;
import com.gg.turnlook.features.webhook.PaymentWebhookEventRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gg.turnlook.features.servicio.Servicio;
import com.gg.turnlook.features.servicio.ServicioRepository;
import com.gg.turnlook.features.sucursal.Sucursal;
import com.gg.turnlook.features.sucursal.SucursalRepository;
import com.gg.turnlook.features.turno.Turno;
import com.gg.turnlook.features.turno.TurnoRepository;
import com.gg.turnlook.features.usuario.Usuario;
import com.gg.turnlook.features.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import({MockPayoutAdapter.class, JpaPayoutStore.class})
class PagosCentralizadosPersistenciaTest {

    @Autowired private PagoRepository pagoRepository;
    @Autowired private PaymentWebhookEventRepository webhookRepository;
    @Autowired private PaymentReleaseRepository releaseRepository;
    @Autowired private FinancialOperationRepository operationRepository;
    @Autowired private PaymentConfigurationRepository configurationRepository;
    @Autowired private PayoutOperationRepository payoutOperationRepository;
    @Autowired private MockPayoutAdapter pagosAdapter;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private SucursalRepository sucursalRepository;
    @Autowired private ServicioRepository servicioRepository;
    @Autowired private TurnoRepository turnoRepository;
    @Autowired private EntityManager entityManager;

    private Sucursal sucursal;
    private Turno turno;

    @BeforeEach
    void prepararContexto() {
        Usuario cliente = usuarioRepository.saveAndFlush(
                new Usuario("Ana", "Paz", "ana-pagos-" + UUID.randomUUID() + "@example.com", "hash"));

        sucursal = new Sucursal("Sucursal Centro", "Calle Pago " + UUID.randomUUID(), null,
                "Sucursal de prueba", LocalTime.of(9, 0), LocalTime.of(18, 0), null, cliente);
        sucursal.setFechaCreacion(LocalDate.now());
        sucursal = sucursalRepository.saveAndFlush(sucursal);

        Servicio servicio = new Servicio("Corte", "Corte de pelo", 30,
                new BigDecimal("1500.00"), sucursal);
        servicio = servicioRepository.saveAndFlush(servicio);

        turno = new Turno(LocalDateTime.now().plusDays(1), cliente, null, servicio);
        turno = turnoRepository.saveAndFlush(turno);
    }

    @Test
    void persistePagoConEstadosInicialesYLoRecuperaPorReferencia() {
        Pago pago = pagoRepository.saveAndFlush(nuevoPago("ref-001", "idem-001"));
        entityManager.clear();

        Pago guardado = pagoRepository.findByExternalReference("ref-001").orElseThrow();

        assertEquals(PaymentState.CREATED, guardado.getPaymentState());
        assertEquals(SettlementState.NOT_DUE, guardado.getSettlementState());
        assertEquals(new BigDecimal("1500.00"), guardado.getAmount());
        assertEquals("ARS", guardado.getCurrency());
        assertEquals(turno.getId(), guardado.getTurno().getId());
        assertEquals(sucursal.getId(), guardado.getSucursal().getId());
        assertNotNull(guardado.getCreatedAt());
    }

    @Test
    void rechazaReferenciaExternaDuplicada() {
        pagoRepository.saveAndFlush(nuevoPago("ref-duplicada", "idem-uniq-1"));

        Pago otro = nuevoPago("ref-duplicada", "idem-uniq-2");
        assertThrows(DataIntegrityViolationException.class, () -> pagoRepository.saveAndFlush(otro));
    }

    @Test
    void rechazaClaveDeIdempotenciaDuplicada() {
        pagoRepository.saveAndFlush(nuevoPago("ref-idem-a", "idem-misma"));

        Pago otro = nuevoPago("ref-idem-b", "idem-misma");
        assertThrows(DataIntegrityViolationException.class, () -> pagoRepository.saveAndFlush(otro));
    }

    @Test
    void deduplicaEventosDeWebhookPorProveedorYEventId() {
        PaymentWebhookEvent evento = new PaymentWebhookEvent();
        evento.setProvider("MERCADOPAGO");
        evento.setEventId("evt-123");
        evento.setTopic("order");
        evento.setAction("order.processed");
        evento.setPayloadHash("abc");
        webhookRepository.saveAndFlush(evento);

        assertTrue(webhookRepository.existsByProviderAndEventId("MERCADOPAGO", "evt-123"));

        PaymentWebhookEvent repetido = new PaymentWebhookEvent();
        repetido.setProvider("MERCADOPAGO");
        repetido.setEventId("evt-123");
        assertThrows(DataIntegrityViolationException.class, () -> webhookRepository.saveAndFlush(repetido));
    }

    @Test
    void rechazaDosLiberacionesConLaMismaClaveDeIdempotencia() {
        Pago pago = pagoRepository.saveAndFlush(nuevoPago("ref-release", "idem-release-pago"));

        releaseRepository.saveAndFlush(nuevaLiberacion(pago, "idem-release-unica"));

        PaymentRelease repetida = nuevaLiberacion(pago, "idem-release-unica");
        assertThrows(DataIntegrityViolationException.class, () -> releaseRepository.saveAndFlush(repetida));
    }

    @Test
    void conservaElOrdenDelLedgerYLosCamposDeHashChain() {
        Pago pago = pagoRepository.saveAndFlush(nuevoPago("ref-ledger", "idem-ledger"));

        FinancialOperation primera = nuevaOperacion(pago, FinancialOperationType.CREATED,
                PaymentState.CREATED, null, "prev-inicial", "hash-1");
        FinancialOperation segunda = nuevaOperacion(pago, FinancialOperationType.DEBIT_REGISTERED,
                SettlementState.HELD_POLICY, "hash-1", "hash-1", "hash-2");
        operationRepository.saveAndFlush(primera);
        operationRepository.saveAndFlush(segunda);
        entityManager.clear();

        var operaciones = operationRepository.findAllByPagoIdOrderByIdAsc(pago.getId());
        assertEquals(2, operaciones.size());
        assertEquals(FinancialOperationType.CREATED, operaciones.get(0).getType());
        assertEquals("hash-1", operaciones.get(1).getPrevHash());
        assertEquals("hash-2", operaciones.get(1).getHash());
        assertNotNull(operaciones.get(0).getOccurredAt());
    }

    @Test
    void guardaConfiguracionPorSucursalYProveedor() {
        PaymentConfiguration configuracion = new PaymentConfiguration();
        configuracion.setSucursal(sucursal);
        configuracion.setProviderCode("MERCADOPAGO");
        configuracion.setEnabledMethods("credit_card,debit_card");
        configuracion.setEnvironment(PaymentEnvironment.TEST);
        configuracion.setActive(true);
        configurationRepository.saveAndFlush(configuracion);

        assertTrue(configurationRepository
                .findBySucursalIdAndProviderCode(sucursal.getId(), "MERCADOPAGO").isPresent());
    }

    @Test
    void derivaLaSucursalDesdeElServicioDelTurno() {
        Pago pago = new Pago();
        pago.setTurno(turno);
        pago.setExternalReference("ref-derivada");
        pago.setAmount(new BigDecimal("1500.00"));
        pago.setIdempotencyKey("idem-derivada");

        pagoRepository.saveAndFlush(pago);
        entityManager.clear();

        Pago guardado = pagoRepository.findById(pago.getId()).orElseThrow();
        assertEquals(sucursal.getId(), guardado.getSucursal().getId());
    }

    @Test
    void cambiarEstadoRegistraAsientoAppendOnlyYEncadenaElHash() {
        Pago pago = pagoRepository.saveAndFlush(nuevoPago("ref-ledger-servicio", "idem-ledger-servicio"));
        FinancialOperationService servicio = new FinancialOperationService(operationRepository);

        FinancialOperation operacion = servicio.cambiarEstado(pago, PaymentState.CAPTURED,
                SettlementState.HELD_POLICY, FinancialOperationType.APPROVED,
                "ORD-123", "sistema");
        entityManager.flush();
        Long asientoId = operacion.getId();
        assertNotNull(asientoId);
        assertEquals("GENESIS", operacion.getPrevHash());

        FinancialOperation segundo = servicio.registrar(pago, FinancialOperationType.DEBIT_REGISTERED,
                PaymentState.CAPTURED + "/HELD_POLICY", PaymentState.CAPTURED + "/HELD_POLICY",
                "ORD-123", "sistema");
        entityManager.flush();
        entityManager.clear();

        Pago actualizado = pagoRepository.findById(pago.getId()).orElseThrow();
        assertEquals(PaymentState.CAPTURED, actualizado.getPaymentState());
        assertEquals(SettlementState.HELD_POLICY, actualizado.getSettlementState());
        assertNotNull(actualizado.getCapturedAt());

        var asientos = operationRepository.findAllByPagoIdOrderByIdAsc(pago.getId());
        assertEquals(2, asientos.size());
        assertEquals(operacion.getHash(), asientos.get(1).getPrevHash());
        assertEquals(segundo.getHash(), asientos.get(1).getHash());

        FinancialOperation inmutable = operationRepository.findById(asientoId).orElseThrow();
        inmutable.setAmount(new BigDecimal("0.01"));
        entityManager.flush();
        entityManager.clear();
        assertEquals(new BigDecimal("1500.00"),
                operationRepository.findById(asientoId).orElseThrow().getAmount());

        RuntimeException bloqueo = assertThrows(RuntimeException.class,
                () -> operationRepository.deleteById(asientoId));
        assertTrue(bloqueo.getMessage().contains("append-only")
                || (bloqueo.getCause() != null
                    && bloqueo.getCause().getMessage().contains("append-only")));
    }

    @Test
    void detectaManipulacionDelLedgerRompiendoLaCadenaDeHashes() {
        Pago pago = pagoRepository.saveAndFlush(nuevoPago("ref-integridad", "idem-integridad"));
        FinancialOperationService servicio = new FinancialOperationService(operationRepository);
        FinancialOperation primera = servicio.cambiarEstado(pago, PaymentState.CAPTURED,
                SettlementState.HELD_POLICY, FinancialOperationType.APPROVED,
                "ORD-INT", "sistema");
        entityManager.flush();
        assertTrue(servicio.verificarIntegridad(pago.getId()));

        // Simula un atacante que modifica la BD directamente, evitando @Immutable.
        entityManager.createNativeQuery(
                        "update financial_operations set hash = 'TAMPER' where id = :id")
                .setParameter("id", primera.getId())
                .executeUpdate();
        entityManager.clear();

        assertFalse(servicio.verificarIntegridad(pago.getId()));
    }

    @Test
    void liquidacionSimuladaPasaDeHeldPolicyAReleasedConElMock() {
        Pago pago = pagoRepository.saveAndFlush(nuevoPago("ref-mock", "idem-mock-pago"));
        pago.setPaymentState(PaymentState.CAPTURED);
        pago.setSettlementState(SettlementState.HELD_POLICY);

        PayoutResult resultado = pagosAdapter.pay(new PayoutCommand(
                pago.getAmount(), sucursal.getId(), "mock-ref-1", "idem-mock-1"));
        FinancialOperationService servicio = new FinancialOperationService(operationRepository);
        servicio.cambiarEstado(pago, PaymentState.CAPTURED, SettlementState.RELEASED,
                FinancialOperationType.RELEASED, resultado.providerReference(), "sistema");
        entityManager.flush();
        entityManager.clear();

        Pago actualizado = pagoRepository.findById(pago.getId()).orElseThrow();
        assertEquals(SettlementState.RELEASED, actualizado.getSettlementState());
        assertEquals(PayoutStatus.PAID, pagosAdapter.getStatus("mock-ref-1"));
        assertTrue(payoutOperationRepository.findByIdempotencyKey("idem-mock-1").isPresent());
        assertEquals(1, operationRepository.findAllByPagoIdOrderByIdAsc(pago.getId()).size());
    }

    private Pago nuevoPago(String referencia, String idempotencia) {
        Pago pago = new Pago();
        pago.setTurno(turno);
        pago.setSucursal(sucursal);
        pago.setExternalReference(referencia);
        pago.setAmount(new BigDecimal("1500.00"));
        pago.setIdempotencyKey(idempotencia);
        return pago;
    }

    private PaymentRelease nuevaLiberacion(Pago pago, String idempotencia) {
        PaymentRelease liberacion = new PaymentRelease();
        liberacion.setPago(pago);
        liberacion.setTurnoId(turno.getId());
        liberacion.setAmount(pago.getAmount());
        liberacion.setReason("Turno realizado");
        liberacion.setIdempotencyKey(idempotencia);
        return liberacion;
    }

    private FinancialOperation nuevaOperacion(Pago pago, FinancialOperationType tipo,
                                              Enum<?> estado, String desde, String prevHash, String hash) {
        FinancialOperation operacion = new FinancialOperation();
        operacion.setPago(pago);
        operacion.setType(tipo);
        operacion.setAmount(pago.getAmount());
        operacion.setFromState(desde);
        operacion.setToState(estado == null ? null : estado.name());
        operacion.setPrevHash(prevHash);
        operacion.setHash(hash);
        operacion.setActor("sistema");
        return operacion;
    }
}
