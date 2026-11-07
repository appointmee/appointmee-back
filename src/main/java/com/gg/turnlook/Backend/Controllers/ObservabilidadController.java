package com.gg.turnlook.Backend.Controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gg.turnlook.Backend.DTO.Observabilidad.NotFoundEventDTO;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;

@RestController
@RequestMapping("/observabilidad")
@CrossOrigin(origins = "*")
public class ObservabilidadController {

    private static final Logger logger = LoggerFactory.getLogger(ObservabilidadController.class);
    private final ObjectMapper objectMapper;

    public ObservabilidadController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostMapping("/404")
    public ResponseEntity<Void> registrar404(@Valid @RequestBody NotFoundEventDTO event) {
        String referrer = limpiarReferrer(event.referrer());
        var logEvent = new NotFoundLogEvent(
                "client_route_not_found",
                event.path(),
                referrer,
                Instant.now().toString()
        );

        try {
            // Una línea JSON facilita su búsqueda y retención en los logs del hosting.
            logger.info("{}", objectMapper.writeValueAsString(logEvent));
        } catch (JsonProcessingException exception) {
            logger.warn("No se pudo serializar un evento de ruta 404", exception);
        }

        return ResponseEntity.accepted().build();
    }

    private String limpiarReferrer(String referrer) {
        if (referrer == null || referrer.isBlank()) {
            return null;
        }

        try {
            URI uri = URI.create(referrer);
            if (!uri.isAbsolute() || uri.getHost() == null) {
                return null;
            }
            return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), null, null)
                    .toString();
        } catch (Exception exception) {
            return null;
        }
    }

    private record NotFoundLogEvent(String event, String path, String referrer, String observedAt) {
    }
}
