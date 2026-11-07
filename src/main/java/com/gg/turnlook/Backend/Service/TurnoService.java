package com.gg.turnlook.Backend.Service;


import com.gg.turnlook.Backend.DTO.Resenia.ReseniaCrearDTO;
import com.gg.turnlook.Backend.DTO.Resenia.ReseniaResponseDTO;
import com.gg.turnlook.Backend.DTO.Servicio.ServicioTurnoResponseDTO;
import com.gg.turnlook.Backend.DTO.Turno.*;
import com.gg.turnlook.Backend.DTO.Usuario.UsuarioResponseDTO;
import com.gg.turnlook.Backend.DTO.Usuario.UsuarioMiniDTO;
import com.gg.turnlook.Backend.Enum.ERol;
import com.gg.turnlook.Backend.Enum.EstadoTurno;
import com.gg.turnlook.Backend.Excepciones.BadRequestException;
import com.gg.turnlook.Backend.Excepciones.ConflictException;
import com.gg.turnlook.Backend.Excepciones.ForbiddenException;
import com.gg.turnlook.Backend.Excepciones.NotFoundException;
import com.gg.turnlook.Backend.Model.*;
import com.gg.turnlook.Backend.Repository.TurnoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class TurnoService {


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

        Usuario empleado = usuarioService.listarUsuarioPorId(turno.getEmpleadoId());

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

        LocalDate dia = turno.getFechaHora().toLocalDate();
        List<Turno> turnosDia = turnoRepo.findByEmpleadoAndFechaHoraBetweenAndEstadoNot(
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
        List<Turno> turnos = turnoRepo.findByEmpleadoAndFechaHoraBetweenAndEstadoNot(
                empleado, iSemana, fSemana, EstadoTurno.CANCELADO);
        Map<LocalDate, List<Turno>> turnosPorDia = turnos.stream()
                .collect(Collectors.groupingBy(t -> t.getFechaHora().toLocalDate()));

        Map<LocalDate, List<LocalTime>> disponibilidad = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            LocalDate dia = LocalDate.now().plusDays(i);

            List<Turno> turnosDia = turnosPorDia.getOrDefault(dia, Collections.emptyList());
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
                                   List<Turno> turnosDia) {

        LocalDateTime finNuevo = inicioNuevo.plusMinutes(duracionServicio);

        for (Turno t : turnosDia) {
            LocalDateTime inicioExistente = t.getFechaHora();
            LocalDateTime finExistente = inicioExistente.plusMinutes(t.getServicio().getDuracion());

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

        // ver si saco o no CONFIRMADO para ver si lo meto o no aca tmb
        if (turno.getEstado() != EstadoTurno.PENDIENTE) {
            throw new ConflictException("El turno no está pendiente");
        }

        turno.setEstado(EstadoTurno.CANCELADO);
        turnoRepo.save(turno);
    }


    public void finalizarTurno(Integer turnoId, String userEmail) {

        Turno turno = listarTurnoPorId(turnoId);

        if (!sucursalService.enSucursal(userEmail, turno.getServicio().getSucursal())) {
            throw new ForbiddenException("No tenes permisos");
        }

        // ver si dejo el estado confirmado y si si agregarlo ak tmb
        if (turno.getEstado() != EstadoTurno.PENDIENTE) {
            throw new ConflictException("El turno no está pendiente");
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

        return turnoRepo.findByServicioSucursalIdAndEstado(sucursal.getId(),
                        estadoTurno)
                .stream()
                .map(t -> new TurnoMiniDTO(t.getId(), t.getServicio().getNombre(),
                        t.getFechaHora(), t.getEstado(),
                        t.getResenia() != null ?
                                t.getResenia().getPuntuacion() : null))
                .toList();
    }


    public List<TurnoMiniDTO> listarTurnosPropiosPorSucursal(
            Integer sucursalId, String empleadoEmail, EstadoTurno estadoTurno){

        Sucursal  sucursal = sucursalService.listarSucursalPorId(sucursalId);

        if(!sucursalService.enSucursal(empleadoEmail, sucursal)){
            throw new ForbiddenException("No tenes permisos");
        }

        return turnoRepo.findByEmpleadoEmailAndServicioSucursalIdAndEstado(
                empleadoEmail, sucursal.getId(), estadoTurno)
                .stream()
                .map(t -> new TurnoMiniDTO(
                        t.getId(), t.getServicio().getNombre(),
                        t.getFechaHora(), t.getEstado(),
                        t.getResenia() != null ?
                                t.getResenia().getPuntuacion() : null))
                .toList();
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
                e.getFotoPerfil().getFotoValida());

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

        return turnoRepo.findByClienteEmail(clienteEmail).stream()
                .map(t -> new TurnoMiniDTO(
                        t.getId(), t.getServicio().getNombre(),
                        t.getFechaHora(), t.getEstado(),
                        t.getResenia() != null ?
                                t.getResenia().getPuntuacion() : null))
                .toList();
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
        return turnoRepo.findById(id)
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


    public void setearPagoRechazado(Integer turnoId) {

        Turno turno = listarTurnoPorId(turnoId);
        turno.setEstado(EstadoTurno.PAGO_RECHAZADO);
        turnoRepo.save(turno);
    }

}



