package com.gg.turnlook.features.solicitud.empleador.dto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ubicacion guardada, lista para que el administrador la abra en Google Maps.
 * La URL es publica: no usa API key y no hace llamadas a Places ni Geocoding.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UbicacionSolicitudResponseDTO {

    private Integer id;
    private String direccion;
    private Double latitud;
    private Double longitud;
    private String googlePlaceId;

    public String getUrlGoogleMaps() {
        if (latitud == null || longitud == null
                || googlePlaceId == null || googlePlaceId.isBlank()) {
            return "";
        }
        String coordenadas = URLEncoder.encode(latitud + "," + longitud, StandardCharsets.UTF_8);
        String placeId = URLEncoder.encode(googlePlaceId.trim(), StandardCharsets.UTF_8);
        return "https://www.google.com/maps/search/?api=1&query=" + coordenadas
                + "&query_place_id=" + placeId;
    }

}
