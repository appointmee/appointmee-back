package com.gg.turnlook.features.pago;

import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;




@RestController
@RequestMapping("/pagos")
public class MercadoPagoController {


    private final MercadoPagoService mpService;


    public MercadoPagoController(MercadoPagoService mpService) {
        this.mpService = mpService;
    }



    /// ENDPOINTS



    @PreAuthorize("hasRole('CLIENTE')")
    @PostMapping("/{turnoId}")
    public ResponseEntity<?> realizarPago(@PathVariable("turnoId") Integer turnoId,
                                          @AuthenticationPrincipal String clienteEmail
    ) throws MPException, MPApiException {

        try {
            return ResponseEntity.ok().body(mpService.pagar(turnoId, clienteEmail));
        } catch (MPException e1) {
            return ResponseEntity.status(500).body("Error mp1");  // test
        } catch (MPApiException e2) {
            return ResponseEntity.status(500).body("Error mp2");  // test
        }
    }


    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String id,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "data.id", required = false) String dataId,
            @RequestBody(required = false) Map<String, Object> payload)
            throws MPException, MPApiException {

        String evento = firstNonBlank(type, topic, stringValue(payload, "type"));
        String pagoId = firstNonBlank(dataId, id, nestedStringValue(payload, "data", "id"));

        // Mercado Pago envía también notificaciones de tópicos que no son pagos.
        if (evento != null && !"payment".equals(evento)) {
            return ResponseEntity.ok().build();
        }

        // Sin ID no hay nada que consultar. Se responde 200 para no reintentar avisos vacíos.
        if (pagoId == null) {
            return ResponseEntity.ok().build();
        }

        final Long paymentId;
        try {
            paymentId = Long.valueOf(pagoId);
        } catch (NumberFormatException exception) {
            return ResponseEntity.badRequest().body("El identificador de pago no es válido");
        }

        try {
            return ResponseEntity.ok().body(mpService.procesarWebhook(paymentId));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String stringValue(Map<String, Object> payload, String key) {
        if (payload == null || payload.get(key) == null) {
            return null;
        }
        return payload.get(key).toString();
    }

    private String nestedStringValue(Map<String, Object> payload, String objectKey, String valueKey) {
        if (payload == null || !(payload.get(objectKey) instanceof Map<?, ?> data)
                || data.get(valueKey) == null) {
            return null;
        }
        return data.get(valueKey).toString();
    }


}
