package com.gg.turnlook.features.turno;

import com.gg.turnlook.common.exception.BadRequestException;
import com.gg.turnlook.common.exception.ConflictException;
import com.gg.turnlook.common.exception.ForbiddenException;
import com.gg.turnlook.common.exception.NotFoundException;
import com.gg.turnlook.features.resenia.Resenia;
import com.gg.turnlook.features.resenia.ReseniaService;
import com.gg.turnlook.features.resenia.dto.ReseniaCrearDTO;
import com.gg.turnlook.features.resenia.dto.ReseniaResponseDTO;
import com.gg.turnlook.features.servicio.Servicio;
import com.gg.turnlook.features.servicio.ServicioService;
import com.gg.turnlook.features.servicio.dto.ServicioTurnoResponseDTO;
import com.gg.turnlook.features.sucursal.Sucursal;
import com.gg.turnlook.features.sucursal.SucursalService;
import com.gg.turnlook.features.turno.dto.DisponibilidadDTO;
import com.gg.turnlook.features.turno.dto.TurnoClienteResponseDTO;
import com.gg.turnlook.features.turno.dto.TurnoCrearDTO;
import com.gg.turnlook.features.turno.dto.TurnoDisponibilidadDTO;
import com.gg.turnlook.features.turno.dto.TurnoMiniDTO;
import com.gg.turnlook.features.turno.dto.TurnoResponseDTO;
import com.gg.turnlook.features.usuario.Usuario;
import com.gg.turnlook.features.usuario.UsuarioService;
import com.gg.turnlook.features.usuario.dto.UsuarioMiniDTO;
import com.gg.turnlook.features.usuario.dto.UsuarioResponseDTO;
import com.gg.turnlook.features.usuario.rol.ERol;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;





@Service
public class TurnoService {


    // Limites de los listados. Se mantiene la respuesta como List para no
    // cambiar el contrato de la API, pero se evita traer historiales completos.
    private static final int LIMITE_TURNOS_CLIENTE = 200;
    private static final int LIMITE_TURNOS_SUCURSAL = 500;


    private final TurnoRepository turnoRepo;
    private final UsuarioService usuarioService;
    private final ServicioService servicioService;
    private final SucursalService sucursalService;
    private final ReseniaService reseniaService;


    public TurnoService(TurnoRepository turnoRepo, UsuarioService usuarioService, ServicioService servicioService, SucursalService sucursalService, ReseniaService reseniaService) {
        this.turnoRepo = turnoRepo;
        this.usuarioService = usuarioService;
        this.servicioService = servicioService;
        this.sucursalService = sucursalService;
        this.reseniaService = reseniaService;
    }


    /// METODOS


    public void registrarTurno(TurnoCrearDTO turno, String clienteEmail) {

        if (!turno.getFechaHora().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("La fecha del turno no puede haber pasado");
        }

        Usuario empleado = usuarioService.listarUsuarioPorIdConRoles(turno.getEmpleadoId());

        if (empleado.getEmail().equals(clienteEmail)) {
            throw new ConflictException("No podes reservar un turno siendo vos el empleado");
        }

        Usuario cliente = usuarioService.listarUsuarioPorEmail(clienteEmail);
        Servicio servicio = servicioService.listarServicioPorId(turno.getServicioId());

        if (empleado.getRoles().stream().noneMatch(r -> r.getRol() == ERol.EMPLEADO ||
                r.getRol() == ERol.EMPLEADOR)) {
            throw new BadRequestException("El usuario elegido no puede atender turnos");
        }

        if (!sucursalService.enSucursal(empleado.getEmail(), servicio.getSucursal())) {
            throw new BadRequestException("El empleado elegido no pertenece a esa sucursal");
        }

        if (!servicio.getSucursal().abreElDia(turno.getFechaHora().toLocalDate().getDayOfWeek())) {
            throw new BadRequestException("La sucursal no abre ese dia");
        }

        LocalDate dia = turno.getFechaHora().toLocalDate();
        List<TurnoDisponibilidadDTO> turnosDia = turnoRepo.findDisponibilidadPorEmpleado(
                empleado, dia.atStartOfDay(), dia.atTime(LocalTime.MAX), EstadoTurno.CANCELADO);

        if (!estaDisponible(turno.getFechaHora(), servicio.getDuracion(), turnosDia)) {
            throw new ConflictException("Ya hay un turno registrado en ese horario");
        }

        Turno turnoFinal = new Turno(turno.getFechaHora(), cliente, empleado, servicio);

        turnoRepo.save(turnoFinal);
    }


    public List<DisponibilidadDTO> verDisponibilidad(Integer empleadoId, Integer servicioId) {

        Usuario empleado = usuarioService.listarUsuarioPorId(empleadoId);
        Servicio servicio = servicioService.listarServicioPorId(servicioId);
        Sucursal sucursal = servicio.getSucursal();

        if (!sucursalService.enSucursal(empleado.getEmail(), sucursal)) {
            throw new BadRequestException("El empleado no pertenece a la sucursal");
        }

        LocalDateTime iSemana = LocalDate.now().atStartOfDay();
        LocalDateTime fSemana = LocalDate.now().plusDays(6).atTime(LocalTime.MAX);
        List<TurnoDisponibilidadDTO> turnos = turnoRepo.findDisponibilidadPorEmpleado(
                empleado, iSemana, fSemana, EstadoTurno.CANCELADO);
        Map<LocalDate, List<TurnoDisponibilidadDTO>> turnosPorDia = turnos.stream()
                .collect(Collectors.groupingBy(t -> t.getFechaHora().toLocalDate()));

        Map<LocalDate, List<LocalTime>> disponibilidad = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            LocalDate dia = LocalDate.now().plusDays(i);

            // si la sucursal no abre ese dia no hay horarios disponibles
            if (!sucursal.abreElDia(dia.getDayOfWeek())) {
                disponibilidad.put(dia, Collections.emptyList());
                continue;
            }

            List<TurnoDisponibilidadDTO> turnosDia = turnosPorDia.getOrDefault(
                    dia, Collections.emptyList());
            List<LocalTime> horariosDisponibles = new ArrayList<>();
            LocalTime apertura = sucursal.getHoraApertura();

            while (!apertura.plusMinutes(servicio.getDuracion()).isAfter(sucursal.getHoraCierre())) {

                LocalDateTime inicioNuevo = LocalDateTime.of(dia, apertura);

                if (inicioNuevo.isBefore(LocalDateTime.now())) {
                    apertura = apertura.plusMinutes(30);
                    continue;
                }

                if (estaDisponible(inicioNuevo, servicio.getDuracion(), turnosDia)) {
                    horariosDisponibles.add(apertura);
                }

                apertura = apertura.plusMinutes(30);
            }
            disponibilidad.put(dia, horariosDisponibles);
        }

        return disponibilidad.entrySet()
                .stream().map(entry -> new DisponibilidadDTO(
                        entry.getKey(), entry.getValue())).toList();
    }


    private boolean estaDisponible(LocalDateTime inicioNuevo, Integer duracionServicio,
                                   List<TurnoDisponibilidadDTO> turnosDia) {

        LocalDateTime finNuevo = inicioNuevo.plusMinutes(duracionServicio);

        for (TurnoDisponibilidadDTO t : turnosDia) {
            LocalDateTime inicioExistente = t.getFechaHora();
            LocalDateTime finExistente = inicioExistente.plusMinutes(t.getDuracionServicio());

            if (inicioNuevo.isBefore(finExistente) && finNuevo.isAfter(inicioExistente)) {
                return false;
            }
        }

        return true;
    }


    // ver si faltan mas validaciones
    public void cancelarTurno(Integer turnoId, String empleadorEmail) {

        Turno turno = listarTurnoPorId(turnoId);

        if (!turno.getServicio().getSucursal().getEmpleador().getEmail()
                .equals(empleadorEmail)) {
            throw new ForbiddenException("No tenes permisos");
        }

        // Se conserva la regla actual: solo se cancelan turnos confirmados.
        if (turno.getEstado() != EstadoTurno.CONFIRMADO) {
            throw new ConflictException("El turno no está confirmado");
        }

        turno.setEstado(EstadoTurno.CANCELADO);
        turnoRepo.save(turno);
    }


    public void finalizarTurno(Integer turnoId, String userEmail) {

        Turno turno = listarTurnoPorId(turnoId);

        if (!sucursalService.enSucursal(userEmail, turno.getServicio().getSucursal())) {
            throw new ForbiddenException("No tenes permisos");
        }

        if (turno.getEstado() != EstadoTurno.CONFIRMADO) {
            throw new ConflictException("El turno no está confirmado");
        }

        if (turno.getFechaHora().isAfter(LocalDateTime.now())) {
            throw new ConflictException("El turno no pasó todavia");
        }

        turno.setEstado(EstadoTurno.REALIZADO);
        turnoRepo.save(turno);
    }


    public List<TurnoMiniDTO> listarTurnosPorSucursal(
            Integer sucursalId, String empleadorEmail, EstadoTurno estadoTurno) {

        Sucursal sucursal = sucursalService.listarSucursalPorId(sucursalId);

        if (!sucursalService.esEmpleadorAca(sucursal, empleadorEmail)) {
            throw new ForbiddenException("No tenes permisos");
        }

        return turnoRepo.findMiniByServicioSucursalIdAndEstado(sucursal.getId(),
                estadoTurno, PageRequest.of(0, LIMITE_TURNOS_SUCURSAL));
    }


    public List<TurnoMiniDTO> listarTurnosPropiosPorSucursal(
            Integer sucursalId, String empleadoEmail, EstadoTurno estadoTurno){

        Sucursal  sucursal = sucursalService.listarSucursalPorId(sucursalId);

        if(!sucursalService.enSucursal(empleadoEmail, sucursal)){
            throw new ForbiddenException("No tenes permisos");
        }

        return turnoRepo.findMiniByEmpleadoEmailAndServicioSucursalIdAndEstado(
                empleadoEmail, sucursal.getId(), estadoTurno,
                PageRequest.of(0, LIMITE_TURNOS_SUCURSAL));
    }


    public TurnoResponseDTO verDetalleTurnoPorSucursal(Integer turnoId, String userEmail) {

        Turno t = listarTurnoPorId(turnoId);

        if (!sucursalService.enSucursal(userEmail, t.getServicio().getSucursal())) {
            throw new ForbiddenException("No tenes permisos");
        }

        if (!sucursalService.esEmpleadorAca(t.getServicio().getSucursal(), userEmail)
                && !t.getEmpleado().getEmail().equals(userEmail)) {
            throw new ForbiddenException("Este turno no esta asignado a vos");
        }

        Usuario c = t.getCliente();
        Usuario e = t.getEmpleado();
        Servicio s = t.getServicio();

        UsuarioMiniDTO cliente = new UsuarioMiniDTO(c.getNombre(), c.getApellido());

        UsuarioResponseDTO empleado = new UsuarioResponseDTO(
                e.getId(), e.getNombre(), e.getApellido(), e.getEmail(),
                e.getFotoPerfil() == null ? null : e.getFotoPerfil().getFotoValida());

        ServicioTurnoResponseDTO servicio = new ServicioTurnoResponseDTO(
                s.getId(), s.getNombre(), s.getPrecio(), s.getDuracion());

        ReseniaResponseDTO resenia = null;

        if (t.getResenia() != null) {
            Resenia r = t.getResenia();
            resenia = new ReseniaResponseDTO(
                    r.getPuntuacion(), r.getComentario(), r.getFechaResenia());
        }

        return new TurnoResponseDTO(
                t.getId(), t.getFechaReserva(), t.getFechaHora(), t.getEstado(),
                cliente, empleado, servicio, resenia);
    }


    public List<TurnoMiniDTO> listarTurnosPorCliente(String clienteEmail) {

        return turnoRepo.findMiniByClienteEmail(clienteEmail,
                PageRequest.of(0, LIMITE_TURNOS_CLIENTE));
    }


    public TurnoClienteResponseDTO verDetalleTurnoPropio(String clienteEmail,
                                                         Integer turnoId) {

        Turno t = listarTurnoPorId(turnoId);

        if (!t.getCliente().getEmail().equals(clienteEmail)) {
            throw new ForbiddenException("No tenes permisos");
        }

        Usuario c = t.getCliente();
        Usuario e = t.getEmpleado();
        Servicio s = t.getServicio();

        UsuarioMiniDTO cl = new UsuarioMiniDTO(c.getNombre(), c.getApellido());

        UsuarioMiniDTO emp = new UsuarioMiniDTO(e.getNombre(), e.getApellido());

        ServicioTurnoResponseDTO serv = new ServicioTurnoResponseDTO(
                s.getId(), s.getNombre(), s.getPrecio(), s.getDuracion());

        ReseniaResponseDTO resenia = null;

        if (t.getResenia() != null) {
            Resenia r = t.getResenia();
            resenia = new ReseniaResponseDTO(
                    r.getPuntuacion(), r.getComentario(), r.getFechaResenia());
        }

        return new TurnoClienteResponseDTO(t.getFechaReserva(), t.getFechaHora(), t.getEstado(),
                emp, serv, cl, resenia);
    }


    public Turno listarTurnoPorId(Integer id) {
        return turnoRepo.findByIdConDetalles(id)
                .orElseThrow(() -> new NotFoundException("No se encontró el turno"));
    }


    public void reseniarTurno(Integer turnoId, ReseniaCrearDTO reseniaDto,
                              String clienteEmail) {

        Turno turno = listarTurnoPorId(turnoId);

        if (!turno.getCliente().getEmail().equals(clienteEmail)) {
            throw new ForbiddenException("No tenes permisos");
        }

        if (turno.getEstado() != EstadoTurno.REALIZADO &&
                turno.getEstado() != EstadoTurno.CANCELADO) {
            throw new ConflictException("Solo se pueden reseñar turnos finalizados");
        }

        if (turno.getResenia() != null) {
            throw new ConflictException("El turno ya fue reseñado");
        }

        reseniaService.guardarResenia(reseniaDto, turno);
    }


    @Transactional
    public EstadoTurno setearPagoAprobado(Integer turnoId) {
        turnoRepo.actualizarEstadoSiEs(turnoId,
                List.of(EstadoTurno.PENDIENTE, EstadoTurno.PAGO_RECHAZADO),
                EstadoTurno.CONFIRMADO);
        return estadoDeTurno(turnoId);
    }

    @Transactional
    public EstadoTurno setearPagoRechazado(Integer turnoId) {
        turnoRepo.actualizarEstadoSiEs(turnoId,
                List.of(EstadoTurno.PENDIENTE), EstadoTurno.PAGO_RECHAZADO);
        return estadoDeTurno(turnoId);
    }

    public EstadoTurno verificarTurnoParaWebhook(Integer turnoId) {
        return estadoDeTurno(turnoId);
    }

    private EstadoTurno estadoDeTurno(Integer turnoId) {
        return turnoRepo.findEstadoById(turnoId)
                .orElseThrow(() -> new NotFoundException("No se encontró el turno"));
    }

}



