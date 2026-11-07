package com.gg.turnlook.Backend.Service;


import com.gg.turnlook.Backend.DTO.SolicitudEmpleador.SolicitudEmpleadorCrearDTO;
import com.gg.turnlook.Backend.DTO.SolicitudEmpleador.SolicitudEmpleadorMiniDTO;
import com.gg.turnlook.Backend.DTO.SolicitudEmpleador.SolicitudEmpleadorResponseDTO;
import com.gg.turnlook.Backend.DTO.Usuario.UsuarioResponseDTO;
import com.gg.turnlook.Backend.Enum.ERol;
import com.gg.turnlook.Backend.Enum.EstadoSolicitud;
import com.gg.turnlook.Backend.Excepciones.ConflictException;
import com.gg.turnlook.Backend.Excepciones.ForbiddenException;
import com.gg.turnlook.Backend.Excepciones.NotFoundException;
import com.gg.turnlook.Backend.Model.Rol;
import com.gg.turnlook.Backend.Model.SolicitudEmpleador;
import com.gg.turnlook.Backend.Model.Usuario;
import com.gg.turnlook.Backend.Repository.SolicitudEmpleadorRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;


@Service
public class SolicitudEmpleadorService {


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

        return solicitudEmpleadorRepo.findByEstado(
                        EstadoSolicitud.PENDIENTE).stream()
                .map(sol -> new SolicitudEmpleadorMiniDTO(
                        sol.getId(), sol.getUsuario().getNombre(),
                        sol.getUsuario().getApellido(), sol.getFechaSolicitud())
                ).toList();
    }


    public List<SolicitudEmpleadorMiniDTO> listarSolicitudesPropias(String userEmail) {

        return solicitudEmpleadorRepo.findByUsuarioEmail(userEmail).
                stream()
                .map(sol -> new SolicitudEmpleadorMiniDTO(
                        sol.getId(), sol.getUsuario().getNombre(),
                        sol.getUsuario().getApellido(), sol.getFechaSolicitud())
                ).toList();
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




