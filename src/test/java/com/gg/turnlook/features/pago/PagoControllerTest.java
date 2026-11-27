package com.gg.turnlook.features.pago;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gg.turnlook.common.exception.BadRequestException;
import com.gg.turnlook.common.exception.ConflictException;
import com.gg.turnlook.common.exception.ForbiddenException;
import com.gg.turnlook.common.exception.GlobalExceptionHandler;
import com.gg.turnlook.common.exception.NotFoundException;
import com.gg.turnlook.features.pago.dto.PagoResponseDTO;
import com.gg.turnlook.features.pago.dto.PayoutRequestDTO;
import com.gg.turnlook.features.pago.dto.RefundRequestDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

class PagoControllerTest {

    private static final String EMAIL = "empleador@example.com";

    private PagoApiService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = Mockito.mock(PagoApiService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PagoController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter,
                                                  ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest,
                                                  WebDataBinderFactory binderFactory) {
                        return EMAIL;
                    }
                })
                .build();
    }

    @Test
    void listaPagosDelUsuario() throws Exception {
        when(service.listarMios(EMAIL)).thenReturn(List.of(dto()));

        mockMvc.perform(get("/pagos/mios"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"id\":1")));

        verify(service).listarMios(EMAIL);
    }

    @Test
    void devuelveDetalleConEstadoFinanciero() throws Exception {
        when(service.detalle(1, EMAIL)).thenReturn(dto());

        mockMvc.perform(get("/pagos/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("HELD_POLICY")));
    }

    @Test
    void pagoInexistenteDa404() throws Exception {
        when(service.detalle(99, EMAIL)).thenThrow(new NotFoundException("Pago no encontrado"));

        mockMvc.perform(get("/pagos/99")).andExpect(status().isNotFound());
    }

    @Test
    void sucursalAjenaDa403() throws Exception {
        when(service.detalle(1, EMAIL)).thenThrow(new ForbiddenException("Sin permisos"));

        mockMvc.perform(get("/pagos/1")).andExpect(status().isForbidden());
    }

    @Test
    void estadoInvalidoDa409() throws Exception {
        when(service.liquidar(eq(1), eq(EMAIL), any())).thenThrow(new ConflictException("No listo"));

        mockMvc.perform(post("/pagos/1/payout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Turno realizado\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void montoInvalidoDa400() throws Exception {
        when(service.reembolsar(eq(1), eq(EMAIL), any()))
                .thenThrow(new BadRequestException("Monto inválido"));

        mockMvc.perform(post("/pagos/1/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10,\"reason\":\"Error\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void liquidaYDevuelveElNuevoEstado() throws Exception {
        PagoResponseDTO liquidado = new PagoResponseDTO(1, 22, 3, "Centro",
                new BigDecimal("1500.00"), "ARS", PaymentState.CAPTURED,
                SettlementState.RELEASED, null, "EXT-1", LocalDateTime.now(), null);
        when(service.liquidar(eq(1), eq(EMAIL), any(PayoutRequestDTO.class))).thenReturn(liquidado);

        mockMvc.perform(post("/pagos/1/payout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Turno realizado\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("RELEASED")));

        verify(service).liquidar(eq(1), eq(EMAIL), any(PayoutRequestDTO.class));
    }

    @Test
    void reembolsaParcialmente() throws Exception {
        PagoResponseDTO reembolsado = new PagoResponseDTO(1, 22, 3, "Centro",
                new BigDecimal("1500.00"), "ARS", PaymentState.CAPTURED,
                SettlementState.REFUNDED, null, "EXT-1", LocalDateTime.now(), null);
        when(service.reembolsar(eq(1), eq(EMAIL), any(RefundRequestDTO.class))).thenReturn(reembolsado);

        mockMvc.perform(post("/pagos/1/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":500.00,\"reason\":\"No asistió\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("REFUNDED")));
    }

    private PagoResponseDTO dto() {
        return new PagoResponseDTO(1, 22, 3, "Centro", new BigDecimal("1500.00"), "ARS",
                PaymentState.CAPTURED, SettlementState.HELD_POLICY, null, "EXT-1",
                LocalDateTime.now(), null);
    }
}
