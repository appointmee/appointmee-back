package com.gg.turnlook.features.pago;

import com.gg.turnlook.features.ledger.FinancialOperationService;
import com.gg.turnlook.features.ledger.FinancialOperationType;
import com.gg.turnlook.features.pago.dto.PagoResponseDTO;
import com.gg.turnlook.features.pago.dto.PayoutRequestDTO;
import com.gg.turnlook.features.pago.dto.RefundRequestDTO;
import com.gg.turnlook.features.payment.PaymentState;
import com.gg.turnlook.features.payment.PayoutProvider;
import com.gg.turnlook.features.payment.SettlementState;
import com.gg.turnlook.features.payment.comandos.PayoutCommand;
import com.gg.turnlook.features.payment.comandos.RefundCommand;
import com.gg.turnlook.features.payment.records.PayoutResult;
import com.gg.turnlook.features.payout.PaymentRelease;
import com.gg.turnlook.features.payout.PaymentReleaseRepository;
import com.gg.turnlook.features.payout.PayoutStatus;
import com.gg.turnlook.features.payout.ReleaseStatus;
import com.gg.turnlook.features.sucursal.Sucursal;
import com.gg.turnlook.features.sucursal.SucursalRepository;
import com.gg.turnlook.features.sucursal.SucursalService;


import com.gg.turnlook.common.exception.BadRequestException;
import com.gg.turnlook.common.exception.ConflictException;
import com.gg.turnlook.common.exception.ForbiddenException;
import com.gg.turnlook.common.exception.NotFoundException;
import com.gg.turnlook.features.sucursal.Sucursal;
import com.gg.turnlook.features.sucursal.SucursalRepository;
import com.gg.turnlook.features.sucursal.SucursalService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Orquestación de consulta, liquidación y reembolso de pagos centralizados. */
@Service
@Transactional(readOnly = true)
public class PagoApiService {

    private final PagoRepository pagoRepository;
    private final PaymentReleaseRepository releaseRepository;
    private final SucursalRepository sucursalRepository;
    private final SucursalService sucursalService;
    private final PayoutProvider payoutProvider;
    private final FinancialOperationService financialOperationService;

    public PagoApiService(PagoRepository pagoRepository,
                          PaymentReleaseRepository releaseRepository,
                          SucursalRepository sucursalRepository,
                          SucursalService sucursalService,
                          PayoutProvider payoutProvider,
                          FinancialOperationService financialOperationService) {
        this.pagoRepository = pagoRepository;
        this.releaseRepository = releaseRepository;
        this.sucursalRepository = sucursalRepository;
        this.sucursalService = sucursalService;
        this.payoutProvider = payoutProvider;
        this.financialOperationService = financialOperationService;
    }

    public List<PagoResponseDTO> listarMios(String email) {
        if (esAdmin()) {
            return pagoRepository.findAll().stream().map(this::mapear).toList();
        }
        if (tieneRol("ROLE_CLIENTE")) {
            return pagoRepository.findByTurnoClienteEmail(email).stream().map(this::mapear).toList();
        }
        List<Integer> sucursales = sucursalRepository.findAll().stream()
                .filter(s -> sucursalService.enSucursal(email, s))
                .map(Sucursal::getId)
                .toList();
        if (sucursales.isEmpty()) {
            return List.of();
        }
        return pagoRepository.findBySucursalIdIn(sucursales).stream().map(this::mapear).toList();
    }

    public PagoResponseDTO detalle(Integer pagoId, String email) {
        Pago pago = buscar(pagoId);
        verificarLectura(pago, email);
        return mapear(pago);
    }

    @Transactional
    public PagoResponseDTO liquidar(Integer pagoId, String email, PayoutRequestDTO request) {
        Pago pago = buscar(pagoId);
        verificarEscritura(pago, email);
        if (pago.getPaymentState() != PaymentState.CAPTURED
                || pago.getSettlementState() != SettlementState.HELD_POLICY) {
            throw new ConflictException("El pago no está listo para liquidar");
        }

        String idempotencia = "payout-" + pago.getId();
        if (releaseRepository.existsByIdempotencyKey(idempotencia)) {
            throw new ConflictException("La liquidación ya fue solicitada");
        }

        PayoutResult resultado = payoutProvider.pay(new PayoutCommand(
                pago.getAmount(),
                pago.getSucursal().getId(),
                "PAYOUT-" + pago.getId(),
                idempotencia));

        PaymentRelease liberacion = new PaymentRelease();
        liberacion.setPago(pago);
        liberacion.setTurnoId(pago.getTurno().getId());
        liberacion.setAmount(pago.getAmount());
        liberacion.setReason(request.reason());
        liberacion.setStatus(ReleaseStatus.RELEASED);
        liberacion.setIdempotencyKey(idempotencia);
        liberacion.setProviderReference(resultado.providerReference());
        liberacion.setRequestedAt(LocalDateTime.now());
        liberacion.setReleasedAt(LocalDateTime.now());
        releaseRepository.save(liberacion);

        financialOperationService.cambiarEstado(pago, PaymentState.CAPTURED,
                SettlementState.RELEASED, FinancialOperationType.RELEASED,
                resultado.providerReference(), email);
        return mapear(pago);
    }

    @Transactional
    public PagoResponseDTO reembolsar(Integer pagoId, String email, RefundRequestDTO request) {
        Pago pago = buscar(pagoId);
        verificarEscritura(pago, email);
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("El monto del reembolso debe ser mayor a cero");
        }
        if (request.amount().compareTo(pago.getAmount()) > 0) {
            throw new BadRequestException("El monto supera el importe del pago");
        }

        String referencia = releaseRepository.findByPagoIdOrderByIdAsc(pago.getId()).stream()
                .map(PaymentRelease::getProviderReference)
                .filter(r -> r != null && !r.isBlank())
                .findFirst()
                .orElseThrow(() -> new ConflictException("No hay una liquidación para reembolsar"));

        String idempotencia = "refund-" + pago.getId() + "-" + request.amount().toPlainString();
        PayoutResult resultado = payoutProvider.refund(new RefundCommand(
                referencia, request.amount(), request.reason(), idempotencia));
        if (resultado.status() != PayoutStatus.REFUNDED && resultado.status() != PayoutStatus.PAID) {
            throw new ConflictException("No se pudo completar el reembolso");
        }

        financialOperationService.cambiarEstado(pago, pago.getPaymentState(),
                SettlementState.REFUNDED, FinancialOperationType.REFUNDED,
                resultado.providerReference(), email);
        return mapear(pago);
    }

    private Pago buscar(Integer pagoId) {
        return pagoRepository.findById(pagoId)
                .orElseThrow(() -> new NotFoundException("Pago no encontrado"));
    }

    private void verificarLectura(Pago pago, String email) {
        if (esAdmin()) {
            return;
        }
        if (pago.getTurno().getCliente() != null
                && email.equals(pago.getTurno().getCliente().getEmail())) {
            return;
        }
        if (sucursalService.enSucursal(email, pago.getSucursal())) {
            return;
        }
        throw new ForbiddenException("No tenes permisos sobre este pago");
    }

    private void verificarEscritura(Pago pago, String email) {
        if (esAdmin() || sucursalService.esEmpleadorAca(pago.getSucursal(), email)) {
            return;
        }
        throw new ForbiddenException("Solo el empleador de la sucursal puede operar sobre este pago");
    }

    private boolean esAdmin() {
        return tieneRol("ROLE_ADMINISTRADOR");
    }

    private boolean tieneRol(String rol) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> rol.equals(authority.getAuthority()));
    }

    private PagoResponseDTO mapear(Pago pago) {
        return new PagoResponseDTO(
                pago.getId(),
                pago.getTurno().getId(),
                pago.getTurno().getFechaHora(),
                pago.getTurno().getServicio() == null ? null : pago.getTurno().getServicio().getNombre(),
                pago.getSucursal().getId(),
                pago.getSucursal().getNombre(),
                pago.getAmount(),
                pago.getCurrency(),
                pago.getPaymentState(),
                pago.getSettlementState(),
                pago.getProviderPaymentId(),
                pago.getExternalReference(),
                pago.getCreatedAt(),
                pago.getCapturedAt());
    }
}
