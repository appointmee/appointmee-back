package com.gg.turnlook.features.chat;

import com.gg.turnlook.features.chat.dto.ConversacionCrearDTO;
import com.gg.turnlook.features.chat.dto.ConversacionDetalleDTO;
import com.gg.turnlook.features.chat.dto.ConversacionResumenDTO;
import com.gg.turnlook.features.chat.dto.MensajeCrearDTO;
import com.gg.turnlook.features.chat.dto.MensajeResponseDTO;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Fachada del chat: mantiene la API del controller y delega en los servicios
 * de conversaciones y mensajes.
 */
@AllArgsConstructor
@Service
public class ChatService {

    private final ConversacionService conversacionService;
    private final MensajeService mensajeService;

    public ConversacionDetalleDTO crear(ConversacionCrearDTO dto, String email) {
        return conversacionService.crear(dto, email);
    }

    public List<ConversacionResumenDTO> listar(String email) {
        return conversacionService.listar(email);
    }

    public ConversacionDetalleDTO obtener(Integer conversacionId, String email) {
        return conversacionService.obtener(conversacionId, email);
    }

    public List<MensajeResponseDTO> listarMensajes(Integer conversacionId, Long beforeId,
                                                  Long afterId, int size, String email) {
        return mensajeService.listar(conversacionId, beforeId, afterId, size, email);
    }

    public MensajeResponseDTO enviar(Integer conversacionId, MensajeCrearDTO dto, String email) {
        return mensajeService.enviar(conversacionId, dto, email);
    }

    public void marcarLeido(Integer conversacionId, String email) {
        mensajeService.marcarLeido(conversacionId, email);
    }
}
