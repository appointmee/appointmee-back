
package com.gg.turnlook.features.verificacion.canalEnvio;

import com.gg.turnlook.features.verificacion.CanalEnvioService;
import com.gg.turnlook.features.verificacion.enums.CanalVerificacion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@Service
public class WhatsappService implements CanalEnvioService {

    private final RestClient cliente;

    public WhatsappService(
            @Value("${whatsapp.gateway.url}") String url,
            @Value("${whatsapp.gateway.api-key}") String apiKey) {

        this.cliente = RestClient.builder()
                .baseUrl(url)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Override
    public CanalVerificacion getCanal() {
        return CanalVerificacion.WHATSAPP;
    }

    @Override
    public void enviarCodigo(String destinatario, String codigo) {

        String mensaje = "Tu código de verificación de AppointMee es: "
                + codigo
                + "\nVence en 5 minutos."
                + "\nSi no lo solicitaste, ignorá este mensaje.";

        cliente.post()
                .uri("/api/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "to", destinatario,
                        "message", mensaje,
                        "eventId", UUID.randomUUID().toString()
                ))
                .retrieve()
                .toBodilessEntity();
    }
}
