package com.gg.turnlook.features.pago;

import com.gg.turnlook.common.exception.ConflictException;
import com.gg.turnlook.common.exception.ForbiddenException;
import com.gg.turnlook.features.payment.PaymentGateway;
import com.gg.turnlook.features.payment.PaymentState;
import com.gg.turnlook.features.payment.records.PagoProveedor;
import com.gg.turnlook.features.turno.EstadoTurno;
import com.gg.turnlook.features.turno.TurnoService;
import org.springframework.stereotype.Service;

/** Fachada del flujo heredado: mantiene POST /pagos/{turnoId} y el webhook. */
@Service
public class MercadoPagoService {

    private final TurnoService turnoService;
    private final PaymentGateway paymentGateway;
    private final PagoRepository pagoRepository;
    private final CobroService cobroService;

    public MercadoPagoService(TurnoService turnoService, PaymentGateway paymentGateway,
                              PagoRepository pagoRepository, CobroService cobroService) {
        this.turnoService = turnoService;
        this.paymentGateway = paymentGateway;
        this.pagoRepository = pagoRepository;
        this.cobroService = cobroService;
    }

    /** Devuelve texto plano (init_point) para compatibilidad con el front actual. */
    public String pagar(Integer turnoId, String clienteEmail) {
        return cobroService.iniciarCobro(turnoId, clienteEmail);
    }

    public String procesarWebhook(Long pagoId) {
        PagoProveedor pago = paymentGateway.consultarPago(String.valueOf(pagoId));
        if (pago.externalReference() == null || pago.externalReference().isBlank()) {
            throw new IllegalArgumentException("El pago no referencia un turno");
        }

        Pago interno = pagoRepository.findByExternalReference(pago.externalReference())
                .orElseThrow(() -> new IllegalArgumentException("El pago no existe en AppointMee"));
        Integer turnoId = interno.getTurno().getId();
        if (pago.estado() == PaymentState.CAPTURED || pago.estado() == PaymentState.FAILED) {
            cobroService.confirmarCobro(interno.getId(), pago.proveedorReferencia(), pago.estado());
        }

        return switch (pago.estado()) {
            case CAPTURED -> {
                EstadoTurno estado = turnoService.setearPagoAprobado(turnoId);
                yield estado == EstadoTurno.CONFIRMADO || estado == EstadoTurno.REALIZADO
                        ? "Pago aprobado. Turno confirmado"
                        : "Pago aprobado, pero el turno no pudo confirmarse por su estado actual";
            }
            case FAILED -> {
                EstadoTurno estado = turnoService.setearPagoRechazado(turnoId);
                yield estado == EstadoTurno.PAGO_RECHAZADO
                        ? "Pago rechazado"
                        : "Resultado de pago recibido; turno no modificado";
            }
            case AUTHORIZED, CREATED -> {
                EstadoTurno estado = turnoService.verificarTurnoParaWebhook(turnoId);
                if (estado == EstadoTurno.CONFIRMADO || estado == EstadoTurno.REALIZADO
                        || estado == EstadoTurno.PAGO_RECHAZADO) {
                    yield "El turno ya tiene un estado final; se ignoró el estado pendiente atrasado";
                }
                yield "Pago pendiente de resolución";
            }
            case CHARGED_BACK -> {
                turnoService.verificarTurnoParaWebhook(turnoId);
                yield "Contracargo recibido; turno sin cambios";
            }
            case REFUNDED, CANCELED -> {
                turnoService.verificarTurnoParaWebhook(turnoId);
                yield "Estado de pago recibido sin cambios";
            }
        };
    }
}
