package com.gg.turnlook.features.pago;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Único punto por el que deben pasar los cambios de estado financiero.
 * Cada transición genera un asiento append-only enlazado al anterior (hash-chain).
 */
@Service
public class FinancialOperationService {

    private static final String GENESIS_HASH = "GENESIS";

    private final FinancialOperationRepository repository;

    public FinancialOperationService(FinancialOperationRepository repository) {
        this.repository = repository;
    }

    /** Registra un asiento sin cambiar el estado del pago (por ejemplo, adeudo registrado). */
    @Transactional
    public FinancialOperation registrar(Pago pago, FinancialOperationType tipo, String desde,
                                        String hasta, String providerReference, String actor) {
        String prevHash = repository.findTopByOrderByIdDesc()
                .map(FinancialOperation::getHash)
                .orElse(GENESIS_HASH);

        FinancialOperation operacion = new FinancialOperation();
        operacion.setPago(pago);
        operacion.setType(tipo);
        operacion.setAmount(pago.getAmount());
        operacion.setFromState(desde);
        operacion.setToState(hasta);
        operacion.setProviderReference(providerReference);
        operacion.setPrevHash(prevHash);
        operacion.setHash(calcularHash(prevHash, pago, tipo, desde, hasta, providerReference));
        operacion.setActor(actor);
        return repository.save(operacion);
    }

    /** Cambia el estado del pago y deja constancia obligatoria en el ledger. */
    @Transactional
    public FinancialOperation cambiarEstado(Pago pago, PaymentState paymentState,
                                             SettlementState settlementState,
                                             FinancialOperationType tipo,
                                             String providerReference, String actor) {
        String desde = pago.getPaymentState().name() + "/" + pago.getSettlementState().name();
        pago.setPaymentState(paymentState);
        pago.setSettlementState(settlementState);
        if (paymentState == PaymentState.CAPTURED && pago.getCapturedAt() == null) {
            pago.setCapturedAt(LocalDateTime.now());
        }
        String hasta = paymentState.name() + "/" + settlementState.name();
        return registrar(pago, tipo, desde, hasta, providerReference, actor);
    }

    private String calcularHash(String prevHash, Pago pago, FinancialOperationType tipo,
                                String desde, String hasta, String providerReference) {
        BigDecimal importe = pago.getAmount() == null ? BigDecimal.ZERO : pago.getAmount();
        String payload = String.join("|",
                prevHash,
                String.valueOf(pago.getId()),
                tipo.name(),
                desde == null ? "" : desde,
                hasta == null ? "" : hasta,
                providerReference == null ? "" : providerReference,
                importe.toPlainString());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
