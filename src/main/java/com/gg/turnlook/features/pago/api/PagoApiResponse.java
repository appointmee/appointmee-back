package com.gg.turnlook.features.pago.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PagoApiResponse(
        String id,
        String status,
        @JsonProperty("status_detail") String statusDetail,
        @JsonProperty("external_reference") String externalReference) {
}
