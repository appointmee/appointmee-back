package com.gg.turnlook.features.mercadopago.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PreferenciaResponse(
        String id,
        @JsonProperty("init_point") String initPoint,
        String status) {
}
