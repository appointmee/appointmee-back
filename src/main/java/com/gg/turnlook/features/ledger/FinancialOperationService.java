package com.gg.turnlook.features.ledger;

import com.gg.turnlook.features.pago.Pago;
import com.gg.turnlook.features.payment.PaymentState;
import com.gg.turnlook.features.payment.SettlementState;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
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
        operacion.setHash(calcularHash(prevHash, pago.getId(), pago.getAmount(),
                tipo, desde, hasta, providerReference));
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

    /** Verifica que la cadena de hashes de un pago no haya sido modificada. */
    @Transactional(readOnly = true)
    public boolean verificarIntegridad(Integer pagoId) {
        List<FinancialOperation> operaciones = repository.findAllByPagoIdOrderByIdAsc(pagoId);
        String esperadoPrevio = GENESIS_HASH;
        for (FinancialOperation operacion : operaciones) {
            if (!esperadoPrevio.equals(operacion.getPrevHash())) {
                return false;
            }
            String esperado = calcularHash(operacion.getPrevHash(),
                    operacion.getPago().getId(), operacion.getAmount(), operacion.getType(),
                    operacion.getFromState(), operacion.getToState(),
                    operacion.getProviderReference());
            if (!esperado.equals(operacion.getHash())) {
                return false;
            }
            esperadoPrevio = operacion.getHash();
        }
        return true;
    }

    private String calcularHash(String prevHash, Integer pagoId, BigDecimal amount,
                                FinancialOperationType tipo, String desde, String hasta,
                                String providerReference) {
        BigDecimal importe = amount == null ? BigDecimal.ZERO : amount;
        String payload = String.join("|",
                prevHash,
                String.valueOf(pagoId),
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
