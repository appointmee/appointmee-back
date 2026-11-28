package com.gg.turnlook.features.mercadopago.api;

import com.gg.turnlook.features.servicio.Servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MercadoPagoRestApiTest {

    @Test
    void laCreacionDePreferenciaLlevaTokenYIdempotencyKey() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.mercadopago.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MercadoPagoRestApi api = new MercadoPagoRestApi(builder.build());

        server.expect(requestTo("https://api.mercadopago.com/checkout/preferences"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer TEST-token"))
                .andExpect(header("X-Idempotency-Key", "idem-http-1"))
                .andRespond(withSuccess("""
                        {"id":"PREF-1","init_point":"https://init","status":"pending"}
                        """, MediaType.APPLICATION_JSON));

        PreferenciaRequest request = new PreferenciaRequest(
                List.of(new PreferenciaRequest.Item("Cobro", "Servicio", 1, "ARS",
                        new BigDecimal("1500.00"))),
                "77", "http://localhost:8080/pagos/webhook",
                new PreferenciaRequest.Payer("cliente@example.com"));

        PreferenciaResponse response = api.crearPreferencia(request, "TEST-token", "idem-http-1");

        assertEquals("https://init", response.initPoint());
        server.verify();
    }

    @Test
    void laConsultaDePagoLlevaToken() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.mercadopago.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MercadoPagoRestApi api = new MercadoPagoRestApi(builder.build());

        server.expect(requestTo("https://api.mercadopago.com/v1/payments/PAGO-1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer TEST-token"))
                .andRespond(withSuccess("""
                        {"id":"PAGO-1","status":"approved","status_detail":"accredited","external_reference":"77"}
                        """, MediaType.APPLICATION_JSON));

        PagoApiResponse response = api.obtenerPago("PAGO-1", "TEST-token");

        assertEquals("approved", response.status());
        server.verify();
    }
}
