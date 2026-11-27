package com.gg.turnlook.features.mercadopago.api;

import com.gg.turnlook.features.pago.Pago;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

/** Cuerpo enviado a la API de Mercado Pago. Solo existe en la capa del adaptador. */
public record PreferenciaRequest(
        List<Item> items,
        @JsonProperty("external_reference") String externalReference,
        @JsonProperty("notification_url") String notificationUrl,
        Payer payer) {

    public record Item(
            String title,
            String description,
            Integer quantity,
            @JsonProperty("currency_id") String currencyId,
            @JsonProperty("unit_price") BigDecimal unitPrice) {
    }

    public record Payer(String email) {
    }
}
