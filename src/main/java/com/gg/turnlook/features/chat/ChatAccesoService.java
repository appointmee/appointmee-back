package com.gg.turnlook.features.chat;

import com.gg.turnlook.common.exception.ForbiddenException;
import com.gg.turnlook.common.exception.NotFoundException;
import com.gg.turnlook.features.turno.EstadoTurno;
import com.gg.turnlook.features.turno.Turno;
import com.gg.turnlook.features.usuario.Usuario;
import java.time.LocalDateTime;
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

    /** Para el interceptor WS: no lanza excepción, solo responde sí/no. */
    public boolean esParticipante(Integer conversacionId, String email) {
        return participanteRepo.findByConversacionIdAndUsuarioEmail(conversacionId, email).isPresent();
    }

    public void crearParticipantes(Conversacion conversacion, Usuario cliente, Usuario profesional) {
        crearParticipante(conversacion, cliente);
        crearParticipante(conversacion, profesional);
    }

    public void guardar(ConversacionParticipante participante) {
        participanteRepo.save(participante);
    }

    /**
     * El chat solo está disponible mientras el turno está activo y hasta 3 días
     * después de finalizado. Se toma `fechaModificacionEstado` como fin del turno.
     */
    public boolean ventanaAbierta(Conversacion conversacion) {
        return conversacion.getTurno() == null || ventanaAbierta(conversacion.getTurno());
    }

    public boolean ventanaAbierta(Turno turno) {
        if (turno == null) return true;

        EstadoTurno estado = turno.getEstado();
        if (estado == EstadoTurno.CANCELADO || estado == EstadoTurno.PAGO_RECHAZADO) return false;
        if (estado != EstadoTurno.REALIZADO) return true;

        LocalDateTime fin = turno.getFechaModificacionEstado() != null
                ? turno.getFechaModificacionEstado()
                : turno.getFechaHora();
        return fin != null && !LocalDateTime.now().isAfter(fin.plusDays(3));
    }

    public void validarVentana(Conversacion conversacion) {
        validarVentana(conversacion.getTurno());
    }

    public void validarVentana(Turno turno) {
        if (!ventanaAbierta(turno)) {
            throw new ForbiddenException(
                    "Esta conversación ya no está disponible. Solo podés chatear durante el turno y hasta 3 días después.");
        }
    }

    private void crearParticipante(Conversacion conversacion, Usuario usuario) {
        ConversacionParticipante participante = new ConversacionParticipante();
        participante.setConversacion(conversacion);
        participante.setUsuario(usuario);
        participanteRepo.save(participante);
    }
}
