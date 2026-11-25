package com.gg.turnlook.features.chat;

import com.gg.turnlook.features.chat.dto.ConversacionDetalleDTO;
import com.gg.turnlook.features.chat.dto.MensajeResponseDTO;
import com.gg.turnlook.features.chat.dto.ParticipanteResponseDTO;
import com.gg.turnlook.features.turno.Turno;
import com.gg.turnlook.features.usuario.Usuario;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Mapeo de entidades a DTOs. El rol no se persiste: se deriva del turno.
 */
@Component
public class ChatMapper {

    public ConversacionDetalleDTO aDetalle(Conversacion conversacion,
                                           List<ConversacionParticipante> participantes) {
        List<ParticipanteResponseDTO> participantesDTO = participantes.stream()
                .map(p -> new ParticipanteResponseDTO(
                        p.getUsuario().getId(),
                        p.getUsuario().getNombre(),
                        p.getUsuario().getApellido(),
                        rolDe(conversacion, p.getUsuario().getId())))
                .toList();

        return new ConversacionDetalleDTO(
                conversacion.getId(),
                conversacion.getSucursal().getId(),
                conversacion.getSucursal().getNombre(),
                conversacion.getTurno() != null ? conversacion.getTurno().getId() : null,
                conversacion.getFechaCreacion(),
                participantesDTO);
    }

    public MensajeResponseDTO aMensaje(Mensaje mensaje) {
        Usuario autor = mensaje.getAutor();
        return new MensajeResponseDTO(
                mensaje.getId(),
                mensaje.getConversacion().getId(),
                autor.getId(),
                autor.getNombre() + " " + autor.getApellido(),
                mensaje.getContenido(),
                mensaje.getFechaCreacion(),
                mensaje.getEstadoEntrega(),
                mensaje.getClienteMensajeId());
    }

    /** El que solicitó el turno es CLIENTE; quien lo desarrolla es EMPLEADO. */
    public String rolDe(Conversacion conversacion, Integer usuarioId) {
        Turno turno = conversacion.getTurno();
        boolean esSolicitante = turno != null && turno.getCliente() != null
                && turno.getCliente().getId().equals(usuarioId);
        return esSolicitante ? "CLIENTE" : "EMPLEADO";
    }
}
