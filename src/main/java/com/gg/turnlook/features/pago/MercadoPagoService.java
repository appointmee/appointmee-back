package com.gg.turnlook.features.pago;

import com.gg.turnlook.common.exception.ConflictException;
import com.gg.turnlook.common.exception.ForbiddenException;
import com.gg.turnlook.features.turno.EstadoTurno;
import com.gg.turnlook.features.turno.Turno;
import com.gg.turnlook.features.turno.TurnoService;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import java.util.Locale;
import org.springframework.stereotype.Service;






@Service
public class MercadoPagoService {



    private final TurnoService turnoService;
    private final MercadoPagoGateway mercadoPagoGateway;


    public MercadoPagoService(TurnoService turnoService, MercadoPagoGateway mercadoPagoGateway) {
        this.turnoService = turnoService;
        this.mercadoPagoGateway = mercadoPagoGateway;
    }



    /// METODOS


    public String pagar(Integer turnoId, String clienteEmail)
            throws MPException, MPApiException {

        Turno turno = turnoService.listarTurnoPorId(turnoId);

        if(!turno.getCliente().getEmail().equals(clienteEmail)){
            throw new ForbiddenException("No tenes permisos");
        }

        if(turno.getEstado() != EstadoTurno.PENDIENTE){
            throw new ConflictException("Solo se pueden pagar turnos pendientes");
        }

        return mercadoPagoGateway.crearPreferencia(turno);
    }


    public String procesarWebhook(Long pagoId) throws MPException, MPApiException {

        PagoMercadoPago pago = mercadoPagoGateway.obtenerPago(pagoId);
        if (pago.externalReference() == null || pago.externalReference().isBlank()) {
            throw new IllegalArgumentException("El pago no referencia un turno");
        }

        final Integer turnoId;
        try {
            turnoId = Integer.valueOf(pago.externalReference());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("La referencia del turno no es válida", exception);
        }

        if (pago.status() == null) {
            throw new IllegalArgumentException("El pago no tiene un estado válido");
        }

        switch (pago.status().toLowerCase(Locale.ROOT)) {
            case "approved" -> {
                EstadoTurno estado = turnoService.setearPagoAprobado(turnoId);
                return estado == EstadoTurno.CONFIRMADO || estado == EstadoTurno.REALIZADO
                        ? "Pago aprobado. Turno confirmado"
                        : "Pago aprobado, pero el turno no pudo confirmarse por su estado actual";
            }
            case "rejected", "cancelled" -> {
                EstadoTurno estado = turnoService.setearPagoRechazado(turnoId);
                return estado == EstadoTurno.PAGO_RECHAZADO
                        ? "Pago rechazado"
                        : "Resultado de pago recibido; turno no modificado";
            }
            case "pending", "in_process", "authorized", "in_mediation" -> {
                EstadoTurno estado = turnoService.verificarTurnoParaWebhook(turnoId);
                if (estado == EstadoTurno.CONFIRMADO || estado == EstadoTurno.REALIZADO) {
                    return "El turno ya está confirmado; se ignoró el estado pendiente atrasado";
                }
                if (estado == EstadoTurno.PAGO_RECHAZADO) {
                    return "El turno ya está rechazado; se ignoró el estado pendiente atrasado";
                }
                return "Pago pendiente de resolución";
            }
            default -> {
                turnoService.verificarTurnoParaWebhook(turnoId);
                return "Estado de pago recibido sin cambios";
            }
        }
    }


}


