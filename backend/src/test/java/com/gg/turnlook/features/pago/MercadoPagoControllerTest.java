package com.gg.turnlook.features.pago;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MercadoPagoControllerTest {

    private MercadoPagoService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = Mockito.mock(MercadoPagoService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new MercadoPagoController(service))
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
                        return "cliente@example.com";
                    }
                })
                .build();
    }

    @Test
    void recibeFormatoActualEnCuerpo() throws Exception {
        when(service.procesarWebhook(987L)).thenReturn("Pago aprobado");

        mockMvc.perform(post("/pagos/webhook")
                        .contentType("application/json")
                        .content("""
                                {"type":"payment","data":{"id":"987"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("Pago aprobado"));

        verify(service).procesarWebhook(987L);
    }

    @Test
    void endpointDeInicioDePagoEntregaElLinkAlCliente() throws Exception {
        when(service.pagar(123, "cliente@example.com")).thenReturn("https://checkout.example.test/preference");

        mockMvc.perform(post("/pagos/123"))
                .andExpect(status().isOk())
                .andExpect(content().string("https://checkout.example.test/preference"));

        verify(service).pagar(123, "cliente@example.com");
    }

    @Test
    void recibeFormatoActualEnQueryString() throws Exception {
        when(service.procesarWebhook(987L)).thenReturn("Pago pendiente");

        mockMvc.perform(post("/pagos/webhook?type=payment&data.id=987"))
                .andExpect(status().isOk())
                .andExpect(content().string("Pago pendiente"));

        verify(service).procesarWebhook(987L);
    }

    @Test
    void conservaCompatibilidadConFormatoAnterior() throws Exception {
        when(service.procesarWebhook(987L)).thenReturn("Pago aprobado");

        mockMvc.perform(post("/pagos/webhook?topic=payment&id=987"))
                .andExpect(status().isOk())
                .andExpect(content().string("Pago aprobado"));

        verify(service).procesarWebhook(987L);
    }

    @Test
    void ignoraEventosQueNoSonPagos() throws Exception {
        mockMvc.perform(post("/pagos/webhook?type=merchant_order&data.id=987"))
                .andExpect(status().isOk());

        verify(service, never()).procesarWebhook(987L);
    }

    @Test
    void rechazaIdsDePagoNoNumericos() throws Exception {
        mockMvc.perform(post("/pagos/webhook?type=payment&data.id=abc"))
                .andExpect(status().isBadRequest());

        verify(service, never()).procesarWebhook(Mockito.anyLong());
    }
}
