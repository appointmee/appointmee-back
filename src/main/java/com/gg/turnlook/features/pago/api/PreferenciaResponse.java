package com.gg.turnlook.features.pago.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PreferenciaResponse(
        String id,
        @JsonProperty("init_point") String initPoint,
        String status) {
}
