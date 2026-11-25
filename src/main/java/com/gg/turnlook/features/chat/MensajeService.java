package com.gg.turnlook.features.chat;

import com.gg.turnlook.common.exception.BadRequestException;
import com.gg.turnlook.features.chat.dto.MensajeCrearDTO;
import com.gg.turnlook.features.chat.dto.MensajeResponseDTO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Envío, historial y estados de entrega de mensajes. */
@AllArgsConstructor
@Service
public class MensajeService {

    private final ConversacionRepository conversacionRepo;
    private final MensajeRepository mensajeRepo;
    private final ChatAccesoService acceso;
    private final ChatMapper mapper;

    @Transactional
    public List<MensajeResponseDTO> listar(Integer conversacionId, Long beforeId,
                                           int size, String email) {
        Conversacion conversacion = acceso.buscarConversacion(conversacionId);
        ConversacionParticipante participante = acceso.participante(conversacion.getId(), email);

        // Al consultar el hilo, los mensajes ajenos pendientes pasan a ENTREGADO.
        mensajeRepo.marcarEntregados(conversacion.getId(), participante.getUsuario().getId(),
                EstadoEntrega.ENVIADO, EstadoEntrega.ENTREGADO);

        int limite = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(0, limite);
        List<Mensaje> mensajes = beforeId == null
                ? mensajeRepo.findByConversacionIdOrderByIdDesc(conversacion.getId(), pageable)
                : mensajeRepo.findByConversacionIdAndIdLessThanOrderByIdDesc(
                        conversacion.getId(), beforeId, pageable);

        List<Mensaje> ordenados = new ArrayList<>(mensajes);
        Collections.reverse(ordenados);
        return ordenados.stream().map(mapper::aMensaje).toList();
    }

    @Transactional
    public MensajeResponseDTO enviar(Integer conversacionId, MensajeCrearDTO dto, String email) {
        Conversacion conversacion = acceso.buscarConversacion(conversacionId);
        ConversacionParticipante participante = acceso.participante(conversacion.getId(), email);

        String contenido = dto.getContenido() == null ? "" : dto.getContenido().trim();
        if (contenido.isEmpty()) {
            throw new BadRequestException("El mensaje no puede estar vacío");
        }
        if (contenido.length() > 2000) {
            throw new BadRequestException("El mensaje no puede superar los 2000 caracteres");
        }
        if (dto.getClienteMensajeId() == null) {
            throw new BadRequestException("Falta el identificador del mensaje");
        }

        return mensajeRepo.findByConversacionIdAndClienteMensajeId(
                        conversacion.getId(), dto.getClienteMensajeId())
                .map(mapper::aMensaje)
                .orElseGet(() -> persistir(conversacion, participante, contenido, dto));
    }

    @Transactional
    public void marcarLeido(Integer conversacionId, String email) {
        Conversacion conversacion = acceso.buscarConversacion(conversacionId);
        ConversacionParticipante participante = acceso.participante(conversacion.getId(), email);

        Long ultimoMensajeId = conversacion.getUltimoMensajeId();
        if (ultimoMensajeId != null
                && (participante.getUltimoMensajeLeidoId() == null
                    || ultimoMensajeId > participante.getUltimoMensajeLeidoId())) {
            mensajeRepo.marcarLeidos(conversacion.getId(), participante.getUsuario().getId(),
                    ultimoMensajeId, EstadoEntrega.LEIDO);
            participante.setUltimoMensajeLeidoId(ultimoMensajeId);
            acceso.guardar(participante);
        }
    }

    private MensajeResponseDTO persistir(Conversacion conversacion,
                                         ConversacionParticipante participante,
                                         String contenido,
                                         MensajeCrearDTO dto) {
        Mensaje mensaje = new Mensaje();
        mensaje.setConversacion(conversacion);
        mensaje.setAutor(participante.getUsuario());
        mensaje.setContenido(contenido);
        mensaje.setEstadoEntrega(EstadoEntrega.ENVIADO);
        mensaje.setClienteMensajeId(dto.getClienteMensajeId());
        mensajeRepo.save(mensaje);

        conversacion.setUltimoMensajeId(mensaje.getId());
        conversacionRepo.save(conversacion);
        return mapper.aMensaje(mensaje);
    }
}
