package com.gg.turnlook.features.chat.ws;

import com.gg.turnlook.features.chat.MensajeService;
import com.gg.turnlook.features.chat.dto.MensajeCrearDTO;
import com.gg.turnlook.features.chat.dto.MensajeResponseDTO;
import java.security.Principal;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/** Entrada STOMP del chat. Persiste antes de reemitir a los participantes. */
@AllArgsConstructor
@Controller
public class ChatWebSocketController {

    private final MensajeService mensajeService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/conversaciones/{conversacionId}/mensajes")
    public void enviar(@DestinationVariable Integer conversacionId,
                       MensajeCrearDTO dto,
                       Principal principal) {
        MensajeResponseDTO mensaje =
                mensajeService.enviar(conversacionId, dto, principal.getName());
        difundir(conversacionId, mensaje);
    }

    @MessageMapping("/conversaciones/{conversacionId}/entregado")
    public void entregado(@DestinationVariable Integer conversacionId,
                          Long mensajeId,
                          Principal principal) {
        MensajeResponseDTO mensaje =
                mensajeService.marcarEntregado(conversacionId, mensajeId, principal.getName());
        difundir(conversacionId, mensaje);
    }

    private void difundir(Integer conversacionId, MensajeResponseDTO mensaje) {
        messagingTemplate.convertAndSend("/topic/conversaciones/" + conversacionId, mensaje);
    }
}
