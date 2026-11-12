package com.gg.turnlook.features.estadisticas;

import com.gg.turnlook.common.exception.BadRequestException;
import com.gg.turnlook.common.exception.ForbiddenException;
import com.gg.turnlook.features.sucursal.Sucursal;
import com.gg.turnlook.features.sucursal.SucursalRepository;
import com.gg.turnlook.features.sucursal.SucursalService;
import com.gg.turnlook.features.turno.EstadoTurno;
import com.gg.turnlook.features.turno.TurnoRepository;
import com.gg.turnlook.features.usuario.UsuarioRepository;
import com.gg.turnlook.features.usuario.rol.ERol;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EstadisticasService {
    private static final List<ERol> ROLES_GRAFICO = List.of(ERol.CLIENTE, ERol.EMPLEADOR, ERol.EMPLEADO);

    private final TurnoRepository turnoRepository;
    private final SucursalService sucursalService;
    private final UsuarioRepository usuarioRepository;
    private final SucursalRepository sucursalRepository;

    public EstadisticasResponseDTO obtener(Integer sucursalId, String empleadorEmail,
                                            LocalDate desde, LocalDate hasta, String agrupacion) {
        String intervalo = validarFiltros(desde, hasta, agrupacion);

        Sucursal sucursal = sucursalService.listarSucursalPorId(sucursalId);
        if (!sucursalService.esEmpleadorAca(sucursal, empleadorEmail)) {
            throw new ForbiddenException("No tenés permisos para consultar estas estadísticas");
        }

        List<Object[]> filas = turnoRepository.findDatosEstadisticas(
                sucursalId, desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
        Map<String, Map<EstadoTurno, Long>> conteos = new LinkedHashMap<>();
        Map<Integer, EstadisticasResponseDTO.DemandaServicio> servicios = new LinkedHashMap<>();
        Map<String, long[]> cancelacionesPorPeriodo = new LinkedHashMap<>();
        long cancelados = 0;
        long concluidos = 0;

        for (Object[] fila : filas) {
            LocalDateTime fecha = (LocalDateTime) fila[0];
            EstadoTurno estado = (EstadoTurno) fila[1];
            Integer servicioIdFila = (Integer) fila[2];
            String nombreServicio = (String) fila[3];
            String periodo = periodo(fecha.toLocalDate(), intervalo);
            Map<EstadoTurno, Long> estados = conteos.computeIfAbsent(periodo, ignored -> estadosVacios());
            estados.compute(estado, (ignored, cantidad) -> cantidad + 1);

            EstadisticasResponseDTO.DemandaServicio demanda = servicios.get(servicioIdFila);
            servicios.put(servicioIdFila, new EstadisticasResponseDTO.DemandaServicio(
                    servicioIdFila, nombreServicio, demanda == null ? 1 : demanda.cantidad() + 1));

            if (estado == EstadoTurno.CANCELADO || estado == EstadoTurno.REALIZADO) {
                long[] acumulado = cancelacionesPorPeriodo.computeIfAbsent(periodo, ignored -> new long[2]);
                acumulado[1]++;
                concluidos++;
                if (estado == EstadoTurno.CANCELADO) {
                    cancelados++;
                    acumulado[0]++;
                }
            }
        }

        List<EstadisticasResponseDTO.TurnosPeriodo> series = conteos.entrySet().stream()
                .map(e -> new EstadisticasResponseDTO.TurnosPeriodo(e.getKey(), e.getValue()))
                .toList();
        List<EstadisticasResponseDTO.CancelacionesPeriodo> serieCancelaciones =
                cancelacionesPorPeriodo.entrySet().stream()
                        .map(e -> new EstadisticasResponseDTO.CancelacionesPeriodo(
                                e.getKey(), e.getValue()[0], e.getValue()[1],
                                tasa(e.getValue()[0], e.getValue()[1])))
                        .toList();
        var noDisponibleOcupacion = new EstadisticasResponseDTO.DatoNoDisponible(false,
                "No se registran horarios laborales por empleado ni bloques de disponibilidad históricos.");
        var noDisponibleIngresos = new EstadisticasResponseDTO.DatoNoDisponible(false,
                "Los pagos no se persisten como transacciones; el precio actual del servicio no representa necesariamente lo cobrado.");
        var noDisponibleAusencias = new EstadisticasResponseDTO.DatoNoDisponible(false,
                "El sistema no tiene un estado o registro explícito de inasistencia.");

        return new EstadisticasResponseDTO(desde, hasta, intervalo, series,
                new ArrayList<>(servicios.values()),
                new EstadisticasResponseDTO.Cancelaciones(cancelados, concluidos,
                        tasa(cancelados, concluidos), serieCancelaciones),
                noDisponibleOcupacion, noDisponibleIngresos, noDisponibleAusencias);
    }

    public EstadisticasResponseDTO.Admin obtenerParaAdmin(LocalDate desde, LocalDate hasta,
                                                           String agrupacion) {
        String intervalo = validarFiltros(desde, hasta, agrupacion);

        Map<String, Map<EstadoTurno, Long>> turnos = new LinkedHashMap<>();
        for (Object[] fila : turnoRepository.findDatosEstadisticasGlobales(
                desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay())) {
            String periodo = periodo(((LocalDateTime) fila[0]).toLocalDate(), intervalo);
            Map<EstadoTurno, Long> estados = turnos.computeIfAbsent(periodo, ignored -> estadosVacios());
            EstadoTurno estado = (EstadoTurno) fila[1];
            estados.compute(estado, (ignored, valor) -> valor + 1);
        }
        List<EstadisticasResponseDTO.AdminTurnosPeriodo> turnosDTO = turnos.entrySet().stream()
                .map(e -> new EstadisticasResponseDTO.AdminTurnosPeriodo(e.getKey(), e.getValue()))
                .toList();

        Map<String, Map<ERol, Long>> altasUsuarios = new LinkedHashMap<>();
        for (Object[] fila : usuarioRepository.findAltasPorFechaYRol(desde, hasta, ROLES_GRAFICO)) {
            String periodo = periodo((LocalDate) fila[0], intervalo);
            Map<ERol, Long> roles = altasUsuarios.computeIfAbsent(periodo, ignored -> rolesVacios());
            ERol rol = (ERol) fila[1];
            roles.compute(rol, (ignored, valor) -> valor + 1);
        }
        List<EstadisticasResponseDTO.UsuariosPeriodo> altasUsuariosDTO = altasUsuarios.entrySet().stream()
                .map(e -> new EstadisticasResponseDTO.UsuariosPeriodo(e.getKey(), e.getValue()))
                .toList();

        Map<ERol, Long> activosPorRol = rolesVacios();
        for (Object[] fila : usuarioRepository.contarUsuariosActivosPorRol(ROLES_GRAFICO)) {
            activosPorRol.put((ERol) fila[0], (Long) fila[1]);
        }

        Map<String, Long> altasSucursales = new LinkedHashMap<>();
        for (Object[] fila : sucursalRepository.findAltasPorFecha(desde, hasta)) {
            String periodo = periodo((LocalDate) fila[0], intervalo);
            altasSucursales.merge(periodo, 1L, Long::sum);
        }
        List<EstadisticasResponseDTO.SucursalesPeriodo> altasSucursalesDTO = altasSucursales.entrySet()
                .stream().map(e -> new EstadisticasResponseDTO.SucursalesPeriodo(e.getKey(), e.getValue()))
                .toList();

        long activas = 0;
        long inactivas = 0;
        for (Object[] fila : sucursalRepository.contarSucursalesPorEstado()) {
            if ((Boolean) fila[0]) activas = (Long) fila[1];
            else inactivas = (Long) fila[1];
        }

        return new EstadisticasResponseDTO.Admin(desde, hasta, intervalo, turnosDTO,
                new EstadisticasResponseDTO.Usuarios(altasUsuariosDTO, activosPorRol),
                new EstadisticasResponseDTO.Sucursales(altasSucursalesDTO, activas, inactivas));
    }

    private static String validarFiltros(LocalDate desde, LocalDate hasta, String agrupacion) {
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            throw new BadRequestException("El rango de fechas es inválido");
        }
        if (hasta.isAfter(desde.plusDays(365))) {
            throw new BadRequestException("El rango máximo permitido es de 366 días");
        }
        String intervalo = agrupacion == null ? "DIA" : agrupacion.toUpperCase();
        if (!intervalo.equals("DIA") && !intervalo.equals("SEMANA") && !intervalo.equals("MES")) {
            throw new BadRequestException("La agrupación debe ser DIA, SEMANA o MES");
        }
        return intervalo;
    }

    private static Map<EstadoTurno, Long> estadosVacios() {
        Map<EstadoTurno, Long> estados = new EnumMap<>(EstadoTurno.class);
        for (EstadoTurno estado : EstadoTurno.values()) estados.put(estado, 0L);
        return estados;
    }

    private static Map<ERol, Long> rolesVacios() {
        Map<ERol, Long> roles = new EnumMap<>(ERol.class);
        ROLES_GRAFICO.forEach(rol -> roles.put(rol, 0L));
        return roles;
    }

    private static String periodo(LocalDate fecha, String agrupacion) {
        return switch (agrupacion) {
            case "SEMANA" -> fecha.with(java.time.DayOfWeek.MONDAY).toString();
            case "MES" -> YearMonth.from(fecha).toString();
            default -> fecha.toString();
        };
    }

    private static BigDecimal tasa(long cantidad, long total) {
        return total == 0 ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(cantidad).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }
}
