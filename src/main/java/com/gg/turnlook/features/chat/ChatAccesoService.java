package com.gg.turnlook.features.chat;

import com.gg.turnlook.common.exception.ForbiddenException;
import com.gg.turnlook.common.exception.NotFoundException;
import com.gg.turnlook.features.usuario.Usuario;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Punto único de acceso a conversaciones y participantes.
 * Centraliza las validaciones de pertenencia para que los demás servicios
 * del chat no necesiten conocer los repositorios de participantes.
 */
@AllArgsConstructor
@Service
public class ChatAccesoService {

    private final ConversacionRepository conversacionRepo;
    private final ConversacionParticipanteRepository participanteRepo;

    public Conversacion buscarConversacion(Integer conversacionId) {
        return conversacionRepo.findById(conversacionId)
                .orElseThrow(() -> new NotFoundException("No se encontró la conversación"));
    }

    public ConversacionParticipante participante(Integer conversacionId, String emailDelParticipante) {
        return participanteRepo.findByConversacionIdAndUsuarioEmail(conversacionId, emailDelParticipante)
                .orElseThrow(() -> new ForbiddenException(
                        "No tenés permisos para acceder a esta conversación"));
    }

    public List<ConversacionParticipante> participantes(Integer conversacionId) {
        return participanteRepo.findByConversacionId(conversacionId);
    }

    public List<ConversacionParticipante> participacionesDe(String emailDelParticipante) {
        return participanteRepo.findByUsuarioEmailOrderByConversacionIdDesc(emailDelParticipante);
    }

    public void crearParticipantes(Conversacion conversacion, Usuario cliente, Usuario profesional) {
        crearParticipante(conversacion, cliente);
        crearParticipante(conversacion, profesional);
    }

    public void guardar(ConversacionParticipante participante) {
        participanteRepo.save(participante);
    }

    private void crearParticipante(Conversacion conversacion, Usuario usuario) {
        ConversacionParticipante participante = new ConversacionParticipante();
        participante.setConversacion(conversacion);
        participante.setUsuario(usuario);
        participanteRepo.save(participante);
    }
}
