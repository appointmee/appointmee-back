package com.gg.turnlook.features.mercadopago.api;

import com.gg.turnlook.features.mercadopago.MercadoPagoProperties;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Implementación REST. Todas las escrituras llevan X-Idempotency-Key. */
@Component
public class MercadoPagoRestApi implements MercadoPagoApi {

    private final RestClient client;

    public MercadoPagoRestApi(MercadoPagoProperties properties) {
        this(RestClient.builder().baseUrl(properties.getApiUrl()).build());
    }

    MercadoPagoRestApi(RestClient client) {
        this.client = client;
    }

    @Override
    public PreferenciaResponse crearPreferencia(PreferenciaRequest request, String accessToken,
                                                String idempotencyKey) {
        return client.post()
                .uri("/checkout/preferences")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Idempotency-Key", idempotencyKey)
                .body(request)
                .retrieve()
                .body(PreferenciaResponse.class);
    }

    @Override
    public PagoApiResponse obtenerPago(String paymentId, String accessToken) {
        return client.get()
                .uri("/v1/payments/{id}", paymentId)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(PagoApiResponse.class);
    }
}
