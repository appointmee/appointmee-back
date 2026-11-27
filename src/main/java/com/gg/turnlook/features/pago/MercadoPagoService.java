package com.gg.turnlook.features.pago;

import com.gg.turnlook.common.exception.ConflictException;
import com.gg.turnlook.common.exception.ForbiddenException;
import com.gg.turnlook.features.servicio.Servicio;
import com.gg.turnlook.features.turno.EstadoTurno;
import com.gg.turnlook.features.turno.Turno;
import com.gg.turnlook.features.turno.TurnoService;
import org.springframework.stereotype.Service;

/** Orquesta el cobro usando solo el puerto de dominio, sin SDK del proveedor. */
@Service
public class MercadoPagoService {

    private final TurnoService turnoService;
    private final PaymentGateway paymentGateway;

    public MercadoPagoService(TurnoService turnoService, PaymentGateway paymentGateway) {
        this.turnoService = turnoService;
        this.paymentGateway = paymentGateway;
    }

    public String pagar(Integer turnoId, String clienteEmail) {
        Turno turno = turnoService.listarTurnoPorId(turnoId);

        if (!turno.getCliente().getEmail().equals(clienteEmail)) {
            throw new ForbiddenException("No tenes permisos");
        }
        if (turno.getEstado() != EstadoTurno.PENDIENTE) {
            throw new ConflictException("Solo se pueden pagar turnos pendientes");
        }

        Servicio servicio = turno.getServicio();
        CobroCreado cobro = paymentGateway.crearCobro(new CrearCobroCommand(
                turnoId,
                servicio.getPrecio(),
                "ARS",
                servicio.getNombre(),
                clienteEmail,
                "pago-turno-" + turnoId));
        return cobro.initPoint();
    }

    public String procesarWebhook(Long pagoId) {
        PagoProveedor pago = paymentGateway.consultarPago(String.valueOf(pagoId));
        if (pago.externalReference() == null || pago.externalReference().isBlank()) {
            throw new IllegalArgumentException("El pago no referencia un turno");
        }

        final Integer turnoId;
        try {
            turnoId = Integer.valueOf(pago.externalReference());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("La referencia del turno no es válida", exception);
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
