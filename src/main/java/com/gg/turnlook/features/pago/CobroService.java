package com.gg.turnlook.features.pago;

import com.gg.turnlook.common.exception.BadRequestException;
import com.gg.turnlook.common.exception.ConflictException;
import com.gg.turnlook.common.exception.ForbiddenException;
import com.gg.turnlook.common.exception.NotFoundException;
import com.gg.turnlook.features.ledger.FinancialOperationService;
import com.gg.turnlook.features.ledger.FinancialOperationType;
import com.gg.turnlook.features.payment.PaymentGateway;
import com.gg.turnlook.features.payment.PaymentState;
import com.gg.turnlook.features.payment.SettlementState;
import com.gg.turnlook.features.payment.comandos.CrearCobroCommand;
import com.gg.turnlook.features.payment.records.CobroCreado;
import com.gg.turnlook.features.servicio.Servicio;
import com.gg.turnlook.features.turno.EstadoTurno;
import com.gg.turnlook.features.turno.Turno;
import com.gg.turnlook.features.turno.TurnoService;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Inicia cobros centralizados y registra el adeudo interno con la sucursal. */
@Service
@Transactional
public class CobroService {

    private static final String MONEDA = "ARS";

    private final PagoRepository pagoRepository;
    private final TurnoService turnoService;
    private final PaymentGateway paymentGateway;
    private final FinancialOperationService ledger;

    public CobroService(PagoRepository pagoRepository, TurnoService turnoService,
                        PaymentGateway paymentGateway, FinancialOperationService ledger) {
        this.pagoRepository = pagoRepository;
        this.turnoService = turnoService;
        this.paymentGateway = paymentGateway;
        this.ledger = ledger;
    }

    /** Crea el pago, llama al proveedor y devuelve el init_point (contrato heredado). */
    public String iniciarCobro(Integer turnoId, String clienteEmail) {
        String idempotencia = "pago-turno-" + turnoId;
        Pago existente = pagoRepository.findByIdempotencyKey(idempotencia).orElse(null);
        if (existente != null) {
            return existente.getCheckoutUrl();
        }

        Turno turno = turnoService.listarTurnoPorId(turnoId);
        if (!turno.getCliente().getEmail().equals(clienteEmail)) {
            throw new ForbiddenException("No tenes permisos");
        }
        if (turno.getEstado() != EstadoTurno.PENDIENTE) {
            throw new ConflictException("Solo se pueden pagar turnos pendientes");
        }

        Servicio servicio = turno.getServicio();
        if (servicio == null || servicio.getPrecio() == null
                || servicio.getPrecio().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("El importe del servicio no es válido");
        }

        Pago pago = new Pago();
        pago.setTurno(turno);
        pago.setSucursal(servicio.getSucursal());
        pago.setAmount(servicio.getPrecio());
        pago.setCurrency(MONEDA);
        pago.setIdempotencyKey(idempotencia);
        pago.setExternalReference("TEMP-" + UUID.randomUUID());
        pago = pagoRepository.saveAndFlush(pago);

        pago.setExternalReference(String.valueOf(pago.getId()));
        CobroCreado cobro = paymentGateway.crearCobro(new CrearCobroCommand(
                pago.getId(),
                pago.getAmount(),
                pago.getCurrency(),
                servicio.getNombre(),
                clienteEmail,
                idempotencia));

        pago.setProviderPaymentId(cobro.proveedorReferencia());
        pago.setCheckoutUrl(cobro.initPoint());
        pago.setProviderStatus(cobro.estado().name());
        pagoRepository.saveAndFlush(pago);

        ledger.registrar(pago, FinancialOperationType.CREATED, null,
                PaymentState.CREATED + "/" + SettlementState.NOT_DUE,
                cobro.proveedorReferencia(), clienteEmail);
        return cobro.initPoint();
    }

    /** Confirma el cobro y registra el adeudo a la sucursal. Idempotente. */
    public void confirmarCobro(Integer pagoId, String proveedorReferencia, PaymentState estado) {
        Pago pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new NotFoundException("Pago no encontrado"));
        if (pago.getPaymentState() == PaymentState.CAPTURED
                && pago.getSettlementState() == SettlementState.HELD_POLICY) {
            return;
        }

        if (estado == PaymentState.CAPTURED) {
            ledger.cambiarEstado(pago, PaymentState.CAPTURED, SettlementState.HELD_POLICY,
                    FinancialOperationType.APPROVED, proveedorReferencia, "webhook");
            ledger.registrar(pago, FinancialOperationType.DEBIT_REGISTERED,
                    PaymentState.CAPTURED + "/" + SettlementState.HELD_POLICY,
                    "ADEUDO_SUCURSAL_" + pago.getSucursal().getId(),
                    proveedorReferencia, "sistema");
        } else if (estado == PaymentState.FAILED) {
            ledger.cambiarEstado(pago, PaymentState.FAILED, SettlementState.NOT_DUE,
                    FinancialOperationType.PAYMENT_FAILED, proveedorReferencia, "webhook");
        }
    }
}
