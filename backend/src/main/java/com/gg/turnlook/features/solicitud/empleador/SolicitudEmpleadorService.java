package com.gg.turnlook.features.solicitud.empleador;

import com.gg.turnlook.common.exception.ConflictException;
import com.gg.turnlook.common.exception.ForbiddenException;
import com.gg.turnlook.common.exception.NotFoundException;
import com.gg.turnlook.features.solicitud.EstadoSolicitud;
import com.gg.turnlook.features.solicitud.empleador.dto.SolicitudEmpleadorCrearDTO;
import com.gg.turnlook.features.solicitud.empleador.dto.SolicitudEmpleadorMiniDTO;
import com.gg.turnlook.features.solicitud.empleador.dto.SolicitudEmpleadorResponseDTO;
import com.gg.turnlook.features.usuario.Usuario;
import com.gg.turnlook.features.usuario.UsuarioService;
import com.gg.turnlook.features.usuario.dto.UsuarioResponseDTO;
import com.gg.turnlook.features.usuario.rol.ERol;
import com.gg.turnlook.features.usuario.rol.Rol;
import com.gg.turnlook.features.usuario.rol.RolService;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;





@Service
public class SolicitudEmpleadorService {


    // Limite de los listados de solicitudes. Se mantiene el retorno List para
    // no cambiar el contrato de la API.
    private static final int LIMITE_SOLICITUDES = 500;


    private final SolicitudEmpleadorRepository solicitudEmpleadorRepo;
    private final UsuarioService usuarioService;
    private final RolService rolService;


    public SolicitudEmpleadorService(SolicitudEmpleadorRepository solicitudEmpleadorRepo, UsuarioService usuarioService, RolService rolService) {
        this.solicitudEmpleadorRepo = solicitudEmpleadorRepo;
        this.usuarioService = usuarioService;
        this.rolService = rolService;
    }


    /// METODOS


    public void registrarSolicitud(SolicitudEmpleadorCrearDTO solicitudEmpleador,
                                   String userEmail) {

        Usuario usuario = usuarioService.listarUsuarioPorEmail(userEmail);

        if (solicitudEmpleadorRepo.existsByUsuarioIdAndEstado(
                usuario.getId(), EstadoSolicitud.PENDIENTE)) {

            throw new ConflictException("Ya tenes una solicitud pendiente");
        }

        SolicitudEmpleador solicitudFinal = new SolicitudEmpleador(
                solicitudEmpleador.getMotivo(), usuario);

        solicitudEmpleadorRepo.save(solicitudFinal);
    }


    public List<SolicitudEmpleadorMiniDTO> listarSolicitudesPendientes() {

        // Consulta anterior a la optimización. Reemplazada por findMiniByEstado.
        // return solicitudEmpleadorRepo.findByEstado(
        //                 EstadoSolicitud.PENDIENTE).stream()
        //         .map(sol -> new SolicitudEmpleadorMiniDTO(
        //                 sol.getId(), sol.getUsuario().getNombre(),
        //                 sol.getUsuario().getApellido(), sol.getFechaSolicitud())
        //         ).toList();
        return solicitudEmpleadorRepo.findMiniByEstado(EstadoSolicitud.PENDIENTE,
                PageRequest.of(0, LIMITE_SOLICITUDES));
    }


    public List<SolicitudEmpleadorMiniDTO> listarSolicitudesPropias(String userEmail) {

        // Consulta anterior a la optimización. Reemplazada por findMiniByUsuarioEmail.
        // return solicitudEmpleadorRepo.findByUsuarioEmail(userEmail).
        //         stream()
        //         .map(sol -> new SolicitudEmpleadorMiniDTO(
        //                 sol.getId(), sol.getUsuario().getNombre(),
        //                 sol.getUsuario().getApellido(), sol.getFechaSolicitud())
        //         ).toList();
        return solicitudEmpleadorRepo.findMiniByUsuarioEmail(userEmail,
                PageRequest.of(0, LIMITE_SOLICITUDES));
    }


    public SolicitudEmpleadorResponseDTO verDetallesSolicitud(Integer solicitudId,
                                                              String userEmail) {

        SolicitudEmpleador sol = listarSolicitudEmpleadorPorId(solicitudId);

        Usuario usuario = usuarioService.listarUsuarioPorEmail(userEmail);

        if (!usuarioService.esAdmin(usuario) &&
                !Objects.equals(sol.getUsuario().getId(), usuario.getId())) {

            throw new ForbiddenException("No tenes permisos");
        }

        return mapearSolicitud(sol, usuario);
    }


    private SolicitudEmpleadorResponseDTO mapearSolicitud(SolicitudEmpleador sol,
                                                          Usuario usuario){

        SolicitudEmpleadorResponseDTO solicitud = new SolicitudEmpleadorResponseDTO();

        solicitud.setId(sol.getId());

        solicitud.setMotivo(sol.getMotivo());

        solicitud.setEstado(sol.getEstado());

        solicitud.setFechaSolicitud(sol.getFechaSolicitud());

        UsuarioResponseDTO user = new UsuarioResponseDTO(
                usuario.getId(), usuario.getNombre(), usuario.getApellido(),
                usuario.getEmail(), usuario.getFotoPerfil().getFotoValida());

        solicitud.setUsuario(user);

        return solicitud;
    }


    public void aprobarSolicitud(Integer solicitudId) {

        SolicitudEmpleador sol = listarSolicitudEmpleadorPorId(solicitudId);

        if (sol.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new ConflictException("Solo se pueden aprobar solicitudes pendientes");
        }

        Rol empleador = rolService.listarPorRol(ERol.EMPLEADOR);

        usuarioService.agregarRol(sol.getUsuario(), empleador);

        sol.setEstado(EstadoSolicitud.APROBADA);
        solicitudEmpleadorRepo.save(sol);
    }


    public void rechazarSolicitud(Integer solicitudId) {

        SolicitudEmpleador sol = listarSolicitudEmpleadorPorId(solicitudId);

        if (sol.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new ConflictException("Solo se pueden rechazar solicitudes pendientes");
        }

        sol.setEstado(EstadoSolicitud.RECHAZADA);
        solicitudEmpleadorRepo.save(sol);
    }


    private SolicitudEmpleador listarSolicitudEmpleadorPorId(Integer solicitudId) {

        return solicitudEmpleadorRepo.findById(solicitudId)
                .orElseThrow(() -> new NotFoundException("No se encontró la solicitud"));
    }



}




