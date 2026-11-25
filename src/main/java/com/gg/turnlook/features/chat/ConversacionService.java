package com.gg.turnlook.features.chat;

import com.gg.turnlook.common.exception.BadRequestException;
import com.gg.turnlook.common.exception.ConflictException;
import com.gg.turnlook.common.exception.ForbiddenException;
import com.gg.turnlook.common.exception.NotFoundException;
import com.gg.turnlook.features.chat.dto.ConversacionCrearDTO;
import com.gg.turnlook.features.chat.dto.ConversacionDetalleDTO;
import com.gg.turnlook.features.chat.dto.ConversacionResumenDTO;
import com.gg.turnlook.features.sucursal.Sucursal;
import com.gg.turnlook.features.turno.Turno;
import com.gg.turnlook.features.turno.TurnoRepository;
import com.gg.turnlook.features.usuario.Usuario;
import com.gg.turnlook.features.usuario.UsuarioService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Alta, listado y detalle de conversaciones. */
@AllArgsConstructor
@Service
public class ConversacionService {

    private final ConversacionRepository conversacionRepo;
    private final MensajeRepository mensajeRepo;
    private final UsuarioService usuarioService;
    private final TurnoRepository turnoRepo;
    private final ChatAccesoService acceso;
    private final ChatMapper mapper;

    /**
     * Crea la conversación de forma idempotente a partir de un turno.
     * Participan quien lo solicita ({@code turno.cliente}) y quien lo desarrolla
     * ({@code turno.empleado}), que puede ser el empleador de la sucursal.
     */
    @Transactional
    public ConversacionDetalleDTO crear(ConversacionCrearDTO dto, String email) {
        if (dto.getTurnoId() == null) {
            throw new BadRequestException("Falta el identificador del turno");
        }

        Usuario actual = usuarioService.listarUsuarioPorEmail(email);
        Turno turno = turnoRepo.findById(dto.getTurnoId())
                .orElseThrow(() -> new NotFoundException("No se encontró el turno"));

        if (turno.getServicio() == null || turno.getServicio().getSucursal() == null) {
            throw new BadRequestException("El turno no tiene una sucursal asociada");
        }
        Sucursal sucursal = turno.getServicio().getSucursal();

        Usuario cliente = turno.getCliente();
        if (cliente == null) {
            throw new BadRequestException("El turno no tiene un cliente asociado");
        }
        Usuario atendedor = turno.getEmpleado();
        if (atendedor == null) {
            throw new ConflictException("El turno no tiene un empleado asignado que lo atienda");
        }
        if (atendedor.getId().equals(cliente.getId())) {
            throw new BadRequestException("El cliente y el empleado no pueden ser la misma persona");
        }

        boolean esCliente = cliente.getId().equals(actual.getId());
        boolean esAtendedor = atendedor.getId().equals(actual.getId());
        if (!esCliente && !esAtendedor) {
            throw new ForbiddenException("No tenés permisos para crear esta conversación");
        }

        Conversacion conversacion = conversacionRepo.findByTurnoId(turno.getId())
                .orElseGet(() -> crearConversacion(sucursal, turno, cliente, atendedor));

        return mapper.aDetalle(conversacion, acceso.participantes(conversacion.getId()));
    }

    @Transactional(readOnly = true)
    public List<ConversacionResumenDTO> listar(String email) {
        Usuario actual = usuarioService.listarUsuarioPorEmail(email);
        List<ConversacionParticipante> participaciones = acceso.participacionesDe(email);

        List<ConversacionResumenDTO> resumenes = new ArrayList<>();
        for (ConversacionParticipante participante : participaciones) {
            Conversacion conversacion = participante.getConversacion();
            Mensaje ultimo = conversacion.getUltimoMensajeId() == null
                    ? null
                    : mensajeRepo.findById(conversacion.getUltimoMensajeId()).orElse(null);

            long noLeidos = participante.getUltimoMensajeLeidoId() == null
                    ? mensajeRepo.countByConversacionIdAndAutorEmailNot(
                            conversacion.getId(), email)
                    : mensajeRepo.countByConversacionIdAndIdGreaterThanAndAutorEmailNot(
                            conversacion.getId(), participante.getUltimoMensajeLeidoId(), email);

            String interlocutor = acceso.participantes(conversacion.getId()).stream()
                    .filter(p -> !p.getUsuario().getId().equals(actual.getId()))
                    .map(p -> p.getUsuario().getNombre() + " " + p.getUsuario().getApellido())
                    .findFirst()
                    .orElse("Sin interlocutor");

            resumenes.add(new ConversacionResumenDTO(
                    conversacion.getId(),
                    conversacion.getSucursal().getId(),
                    conversacion.getSucursal().getNombre(),
                    conversacion.getTurno() != null ? conversacion.getTurno().getId() : null,
                    conversacion.getFechaCreacion(),
                    ultimo != null ? ultimo.getContenido() : null,
                    ultimo != null ? ultimo.getFechaCreacion() : null,
                    noLeidos,
                    interlocutor,
                    mapper.rolDe(conversacion, actual.getId())));
        }

        resumenes.sort(Comparator.comparing(
                ConversacionResumenDTO::getUltimoMensajeFecha,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return resumenes;
    }

    @Transactional(readOnly = true)
    public ConversacionDetalleDTO obtener(Integer conversacionId, String email) {
        Conversacion conversacion = acceso.buscarConversacion(conversacionId);
        acceso.participante(conversacion.getId(), email);
        return mapper.aDetalle(conversacion, acceso.participantes(conversacion.getId()));
    }

    private Conversacion crearConversacion(Sucursal sucursal, Turno turno,
                                           Usuario cliente, Usuario atendedor) {
        Conversacion conversacion = new Conversacion();
        conversacion.setSucursal(sucursal);
        conversacion.setTurno(turno);
        conversacion.setCliente(cliente);
        conversacionRepo.save(conversacion);

        acceso.crearParticipantes(conversacion, cliente, atendedor);
        return conversacion;
    }
}
