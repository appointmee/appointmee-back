package com.gg.turnlook.features.solicitud.empleador;

import com.gg.turnlook.common.exception.BadRequestException;
import com.gg.turnlook.common.exception.ConflictException;
import com.gg.turnlook.common.exception.ForbiddenException;
import com.gg.turnlook.common.exception.NotFoundException;
import com.gg.turnlook.features.solicitud.EstadoSolicitud;
import com.gg.turnlook.features.solicitud.empleador.dto.PaginaSolicitudesEmpleadorDTO;
import com.gg.turnlook.features.solicitud.empleador.dto.SolicitudEmpleadorCrearDTO;
import com.gg.turnlook.features.solicitud.empleador.dto.SolicitudEmpleadorMiniDTO;
import com.gg.turnlook.features.solicitud.empleador.dto.SolicitudEmpleadorResponseDTO;
import com.gg.turnlook.features.solicitud.empleador.dto.UbicacionSolicitudDTO;
import com.gg.turnlook.features.solicitud.empleador.dto.UbicacionSolicitudResponseDTO;
import com.gg.turnlook.features.usuario.FotoPerfilUsuario;
import com.gg.turnlook.features.usuario.Usuario;
import com.gg.turnlook.features.usuario.UsuarioService;
import com.gg.turnlook.features.usuario.dto.UsuarioResponseDTO;
import com.gg.turnlook.features.usuario.rol.ERol;
import com.gg.turnlook.features.usuario.rol.Rol;
import com.gg.turnlook.features.usuario.rol.RolService;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;




@Service
public class SolicitudEmpleadorService {


    // Limite del listado propio (no administrativo, que ahora es paginado).
    private static final int LIMITE_SOLICITUDES = 500;
    private static final int TAMANO_DEFECTO = 10;
    private static final int MOTIVO_MINIMO = 20;
    private static final int MOTIVO_MAXIMO = 3000;
    private static final int DIRECCION_MAXIMA = 500;
    private static final int PLACE_ID_MAXIMO = 255;
    private static final int UBICACIONES_MAXIMAS = 10;


    private final SolicitudEmpleadorRepository solicitudEmpleadorRepo;
    private final UsuarioService usuarioService;
    private final RolService rolService;


    public SolicitudEmpleadorService(SolicitudEmpleadorRepository solicitudEmpleadorRepo, UsuarioService usuarioService, RolService rolService) {
        this.solicitudEmpleadorRepo = solicitudEmpleadorRepo;
        this.usuarioService = usuarioService;
        this.rolService = rolService;
    }


    /// METODOS


    @Transactional
    public void registrarSolicitud(SolicitudEmpleadorCrearDTO solicitudEmpleador,
                                   String userEmail) {

        validarMotivo(solicitudEmpleador.getMotivo());
        validarUbicaciones(solicitudEmpleador.getUbicaciones());

        Usuario usuario = usuarioService.listarUsuarioPorEmail(userEmail);

        if (solicitudEmpleadorRepo.existsByUsuarioIdAndEstado(
                usuario.getId(), EstadoSolicitud.PENDIENTE)) {

            throw new ConflictException("Ya tenes una solicitud pendiente");
        }

        SolicitudEmpleador solicitudFinal = new SolicitudEmpleador(
                solicitudEmpleador.getMotivo().trim(), usuario);

        for (UbicacionSolicitudDTO ubicacion : solicitudEmpleador.getUbicaciones()) {
            solicitudFinal.agregarUbicacion(new UbicacionSolicitud(
                    ubicacion.getDireccion().trim(),
                    ubicacion.getLatitud(),
                    ubicacion.getLongitud(),
                    ubicacion.getGooglePlaceId().trim()));
        }

        solicitudEmpleadorRepo.save(solicitudFinal);
    }


    /// Listado administrativo paginado: sin filtro devuelve todos los estados,
    /// incluidos los historicos. El filtro y el OFFSET/LIMIT se resuelven en
    /// PostgreSQL; solo se traen los registros de la pagina pedida.
    /// Los parametros se reciben como texto para responder 400 (y no 500) cuando
    /// no son numeros enteros.
    @Transactional(readOnly = true)
    public PaginaSolicitudesEmpleadorDTO listarSolicitudesAdministrativas(
            String estado, String pagina, String size) {

        EstadoSolicitud filtro = parsearEstado(estado);
        int numeroPagina = parsearNumero(pagina, "page", 0);
        int tamanoPagina = parsearNumero(size, "size", TAMANO_DEFECTO);
        validarPaginacion(numeroPagina, tamanoPagina);

        Page<SolicitudEmpleadorMiniDTO> resultado = solicitudEmpleadorRepo.findMiniAdmin(
                filtro, PageRequest.of(numeroPagina, tamanoPagina));

        return new PaginaSolicitudesEmpleadorDTO(
                resultado.getContent(),
                resultado.getTotalElements(),
                resultado.getTotalPages(),
                resultado.getNumber(),
                resultado.getSize());
    }


    @Transactional(readOnly = true)
    public List<SolicitudEmpleadorMiniDTO> listarSolicitudesPropias(String userEmail) {

        return solicitudEmpleadorRepo.findMiniByUsuarioEmail(userEmail,
                PageRequest.of(0, LIMITE_SOLICITUDES));
    }


    @Transactional(readOnly = true)
    public SolicitudEmpleadorResponseDTO verDetallesSolicitud(Integer solicitudId,
                                                              String userEmail) {

        SolicitudEmpleador sol = solicitudEmpleadorRepo.findByIdConUbicaciones(solicitudId)
                .orElseThrow(() -> new NotFoundException("No se encontró la solicitud"));

        Usuario usuario = usuarioService.listarUsuarioPorEmail(userEmail);
        Usuario solicitante = sol.getUsuario();

        if (!usuarioService.esAdmin(usuario) &&
                (solicitante == null || !Objects.equals(solicitante.getId(), usuario.getId()))) {

            throw new ForbiddenException("No tenes permisos");
        }

        return mapearSolicitud(sol, solicitante);
    }


    private SolicitudEmpleadorResponseDTO mapearSolicitud(SolicitudEmpleador sol,
                                                          Usuario solicitante){

        SolicitudEmpleadorResponseDTO solicitud = new SolicitudEmpleadorResponseDTO();

        solicitud.setId(sol.getId());

        solicitud.setMotivo(sol.getMotivo());

        solicitud.setEstado(sol.getEstado());

        solicitud.setFechaSolicitud(sol.getFechaSolicitud());

        if (solicitante != null) {
            UsuarioResponseDTO user = new UsuarioResponseDTO(
                    solicitante.getId(), solicitante.getNombre(), solicitante.getApellido(),
                    solicitante.getEmail(), FotoPerfilUsuario.resolver(solicitante.getFotoPerfil()));
            solicitud.setUsuario(user);
        }

        // Las solicitudes historicas no tienen ubicaciones: devuelven una
        // coleccion vacia en lugar de fallar.
        List<UbicacionSolicitudResponseDTO> ubicaciones = sol.getUbicaciones() == null
                ? List.of()
                : sol.getUbicaciones().stream()
                        .map(u -> new UbicacionSolicitudResponseDTO(
                                u.getId(), u.getDireccion(), u.getLatitud(),
                                u.getLongitud(), u.getGooglePlaceId()))
                        .toList();
        solicitud.setUbicaciones(ubicaciones);

        return solicitud;
    }


    @Transactional
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


    @Transactional
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


    private EstadoSolicitud parsearEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        try {
            return EstadoSolicitud.valueOf(estado.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    "Estado inválido. Los valores admitidos son PENDIENTE, APROBADA y RECHAZADA");
        }
    }


    private int parsearNumero(String valor, String nombreParametro, int valorPorDefecto) {
        if (valor == null || valor.isBlank()) {
            return valorPorDefecto;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException(
                    "El parámetro " + nombreParametro + " debe ser un número entero");
        }
    }


    private void validarPaginacion(int pagina, int tamano) {
        if (pagina < 0) {
            throw new BadRequestException("El parámetro page no puede ser negativo");
        }
        if (tamano < 1) {
            throw new BadRequestException("El parámetro size debe ser mayor que 0");
        }
    }


    private void validarMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new BadRequestException("El motivo es obligatorio");
        }
        if (motivo.length() < MOTIVO_MINIMO || motivo.length() > MOTIVO_MAXIMO) {
            throw new BadRequestException(
                    "El motivo debe tener entre " + MOTIVO_MINIMO + " y "
                            + MOTIVO_MAXIMO + " caracteres");
        }
    }


    /// Validacion en servicio ademas de la validacion del DTO: entre 1 y 10
    /// ubicaciones, ninguna parcial, fuera de rango o con campos vacios.
    private void validarUbicaciones(List<UbicacionSolicitudDTO> ubicaciones) {
        if (ubicaciones == null || ubicaciones.isEmpty()) {
            throw new BadRequestException("Debe incluir al menos una ubicacion");
        }
        if (ubicaciones.size() > UBICACIONES_MAXIMAS) {
            throw new BadRequestException(
                    "Una solicitud puede incluir como maximo "
                            + UBICACIONES_MAXIMAS + " ubicaciones");
        }
        for (UbicacionSolicitudDTO ubicacion : ubicaciones) {
            if (ubicacion == null) {
                throw new BadRequestException("Hay una ubicacion incompleta");
            }
            if (ubicacion.getDireccion() == null || ubicacion.getDireccion().isBlank()) {
                throw new BadRequestException("La direccion de la ubicacion es obligatoria");
            }
            if (ubicacion.getDireccion().length() > DIRECCION_MAXIMA) {
                throw new BadRequestException("La direccion no puede superar los "
                        + DIRECCION_MAXIMA + " caracteres");
            }
            validarCoordenada(ubicacion.getLatitud(), -90, 90, "latitud");
            validarCoordenada(ubicacion.getLongitud(), -180, 180, "longitud");
            if (ubicacion.getGooglePlaceId() == null
                    || ubicacion.getGooglePlaceId().isBlank()) {
                throw new BadRequestException("El Google Place ID es obligatorio");
            }
            if (ubicacion.getGooglePlaceId().length() > PLACE_ID_MAXIMO) {
                throw new BadRequestException("El Google Place ID no puede superar los "
                        + PLACE_ID_MAXIMO + " caracteres");
            }
        }
    }


    private void validarCoordenada(Double valor, double minimo, double maximo, String nombre) {
        if (valor == null || Double.isNaN(valor) || Double.isInfinite(valor)
                || valor < minimo || valor > maximo) {
            throw new BadRequestException("La " + nombre + " debe estar entre "
                    + minimo + " y " + maximo);
        }
    }


}
