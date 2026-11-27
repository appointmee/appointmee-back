package com.gg.turnlook.features.pago;

import com.gg.turnlook.features.pago.comandos.PayoutCommand;
import com.gg.turnlook.features.pago.comandos.RefundCommand;
import com.gg.turnlook.features.pago.records.PayoutResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gg.turnlook.common.exception.BadRequestException;
import com.gg.turnlook.common.exception.ConflictException;
import com.gg.turnlook.common.exception.ForbiddenException;
import com.gg.turnlook.features.pago.dto.PagoResponseDTO;
import com.gg.turnlook.features.pago.dto.PayoutRequestDTO;
import com.gg.turnlook.features.pago.dto.RefundRequestDTO;
import com.gg.turnlook.features.servicio.Servicio;
import com.gg.turnlook.features.sucursal.Sucursal;
import com.gg.turnlook.features.sucursal.SucursalRepository;
import com.gg.turnlook.features.sucursal.SucursalService;
import com.gg.turnlook.features.turno.Turno;
import com.gg.turnlook.features.usuario.Usuario;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class PagoApiServiceTest {

    private PagoRepository pagoRepository;
    private PaymentReleaseRepository releaseRepository;
    private SucursalRepository sucursalRepository;
    private SucursalService sucursalService;
    private PayoutProvider payoutProvider;
    private FinancialOperationService financialOperationService;
    private PagoApiService service;

    private Pago pago;

    @BeforeEach
    void setUp() {
        pagoRepository = mock(PagoRepository.class);
        releaseRepository = mock(PaymentReleaseRepository.class);
        sucursalRepository = mock(SucursalRepository.class);
        sucursalService = mock(SucursalService.class);
        payoutProvider = mock(PayoutProvider.class);
        financialOperationService = mock(FinancialOperationService.class);
        service = new PagoApiService(pagoRepository, releaseRepository, sucursalRepository,
                sucursalService, payoutProvider, financialOperationService);

        Sucursal sucursal = new Sucursal();
        sucursal.setId(3);
        sucursal.setNombre("Sucursal Centro");

        Usuario cliente = new Usuario("Ana", "Paz", "cliente@example.com", "hash");
        cliente.setId(11);

        Servicio servicio = new Servicio();
        servicio.setNombre("Corte de pelo");
        servicio.setSucursal(sucursal);

        Turno turno = new Turno();
        turno.setId(22);
        turno.setFechaHora(LocalDateTime.of(2026, 12, 1, 10, 0));
        turno.setCliente(cliente);
        turno.setServicio(servicio);

        pago = new Pago();
        pago.setId(1);
        pago.setTurno(turno);
        pago.setSucursal(sucursal);
        pago.setAmount(new BigDecimal("1500.00"));
        pago.setCurrency("ARS");
        pago.setPaymentState(PaymentState.CAPTURED);
        pago.setSettlementState(SettlementState.HELD_POLICY);
        pago.setExternalReference("EXT-1");
        pago.setCreatedAt(LocalDateTime.now());

        when(pagoRepository.findById(1)).thenReturn(Optional.of(pago));
        doAnswer(invocation -> {
            Pago destino = invocation.getArgument(0);
            SettlementState nuevo = invocation.getArgument(2);
            destino.setSettlementState(nuevo);
            return null;
        }).when(financialOperationService).cambiarEstado(any(), any(), any(), any(), any(), any());
    }

    @AfterEach
    void limpiarSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void clientePropietarioPuedeVerSuPago() {
        conRol("ROLE_CLIENTE");

        PagoResponseDTO dto = service.detalle(1, "cliente@example.com");

        assertEquals(1, dto.id());
        assertEquals(22, dto.turnoId());
        assertEquals("Corte de pelo", dto.nombreServicio());
        assertEquals(2026, dto.fechaTurno().getYear());
        assertEquals(PaymentState.CAPTURED, dto.paymentState());
        assertEquals(SettlementState.HELD_POLICY, dto.settlementState());
    }

    @Test
    void administradorPuedeVerCualquierPago() {
        conRol("ROLE_ADMINISTRADOR");

        assertEquals(1, service.detalle(1, "otro@example.com").id());
    }

    @Test
    void empleadorDeOtraSucursalNoPuedeVerElPago() {
        conRol("ROLE_EMPLEADOR");
        when(sucursalService.enSucursal("otro@example.com", pago.getSucursal())).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> service.detalle(1, "otro@example.com"));
    }

    @Test
    void liquidarRechazaPagoQueNoEstaRetenido() {
        conRol("ROLE_EMPLEADOR");
        when(sucursalService.esEmpleadorAca(pago.getSucursal(), "empleador@example.com"))
                .thenReturn(true);
        pago.setSettlementState(SettlementState.NOT_DUE);

        assertThrows(ConflictException.class, () -> service.liquidar(1, "empleador@example.com",
                new PayoutRequestDTO("Turno realizado")));
        verify(payoutProvider, never()).pay(any(PayoutCommand.class));
    }

    @Test
    void liquidarGeneraOrdenAsientoYCambiaEstado() {
        conRol("ROLE_EMPLEADOR");
        when(sucursalService.esEmpleadorAca(pago.getSucursal(), "empleador@example.com"))
                .thenReturn(true);
        when(releaseRepository.existsByIdempotencyKey("payout-1")).thenReturn(false);
        when(payoutProvider.pay(any(PayoutCommand.class))).thenReturn(new PayoutResult(
                "PAYOUT-1", PayoutStatus.PAID, "MOCK-1", LocalDateTime.now()));

        PagoResponseDTO dto = service.liquidar(1, "empleador@example.com",
                new PayoutRequestDTO("Turno realizado"));

        assertEquals(SettlementState.RELEASED, dto.settlementState());
        verify(payoutProvider).pay(any(PayoutCommand.class));
        verify(releaseRepository).save(any(PaymentRelease.class));
        verify(financialOperationService).cambiarEstado(any(Pago.class), any(PaymentState.class),
                any(SettlementState.class), any(FinancialOperationType.class), any(), any());
    }

    @Test
    void liquidarDuplicadaDaConflicto() {
        conRol("ROLE_ADMINISTRADOR");
        when(releaseRepository.existsByIdempotencyKey("payout-1")).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.liquidar(1, "admin@example.com",
                new PayoutRequestDTO("Turno realizado")));
        verify(payoutProvider, never()).pay(any(PayoutCommand.class));
    }

    @Test
    void reembolsarRechazaMontoMayorAlImporte() {
        conRol("ROLE_ADMINISTRADOR");

        assertThrows(BadRequestException.class, () -> service.reembolsar(1, "admin@example.com",
                new RefundRequestDTO(new BigDecimal("2000.00"), "Error")));
    }

    @Test
    void reembolsarSinLiquidacionPreviaDaConflicto() {
        conRol("ROLE_ADMINISTRADOR");
        when(releaseRepository.findByPagoIdOrderByIdAsc(1)).thenReturn(List.of());

        assertThrows(ConflictException.class, () -> service.reembolsar(1, "admin@example.com",
                new RefundRequestDTO(new BigDecimal("500.00"), "No asistió")));
    }

    @Test
    void reembolsarParcialCambiaEstadoYCuentaEnLedger() {
        conRol("ROLE_ADMINISTRADOR");
        PaymentRelease liberacion = new PaymentRelease();
        liberacion.setProviderReference("MOCK-1");
        when(releaseRepository.findByPagoIdOrderByIdAsc(1)).thenReturn(List.of(liberacion));
        when(payoutProvider.refund(any(RefundCommand.class))).thenReturn(new PayoutResult(
                "PAYOUT-1", PayoutStatus.REFUNDED, "MOCK-REFUND-1", LocalDateTime.now()));

        PagoResponseDTO dto = service.reembolsar(1, "admin@example.com",
                new RefundRequestDTO(new BigDecimal("500.00"), "No asistió"));

        assertEquals(SettlementState.REFUNDED, dto.settlementState());
        verify(payoutProvider).refund(any(RefundCommand.class));
        verify(financialOperationService).cambiarEstado(any(Pago.class), any(PaymentState.class),
                any(SettlementState.class), any(FinancialOperationType.class), any(), any());
    }

    private void conRol(String rol) {
        var authentication = new UsernamePasswordAuthenticationToken("usuario@example.com", null,
                List.of(new SimpleGrantedAuthority(rol)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
