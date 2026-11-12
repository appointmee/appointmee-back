package com.gg.turnlook.features.estadisticas;

import com.gg.turnlook.features.turno.EstadoTurno;
import com.gg.turnlook.features.usuario.rol.ERol;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.Value;

@Value
public class EstadisticasResponseDTO {
    LocalDate desde;
    LocalDate hasta;
    String agrupacion;
    List<TurnosPeriodo> turnosPorPeriodo;
    List<DemandaServicio> demandaPorServicio;
    Cancelaciones cancelaciones;
    DatoNoDisponible ocupacion;
    DatoNoDisponible ingresos;
    DatoNoDisponible ausencias;

    public record TurnosPeriodo(String periodo, Map<EstadoTurno, Long> estados) {}
    public record DemandaServicio(Integer servicioId, String servicio, long cantidad) {}
    public record Cancelaciones(long cantidad, long turnosConcluidos, BigDecimal tasa,
                                List<CancelacionesPeriodo> porPeriodo) {}
    public record CancelacionesPeriodo(String periodo, long cancelados,
                                       long turnosConcluidos, BigDecimal tasa) {}
    public record DatoNoDisponible(boolean disponible, String motivo) {}

    public record Admin(LocalDate desde, LocalDate hasta, String agrupacion,
                        List<AdminTurnosPeriodo> turnosPorPeriodo,
                        Usuarios usuarios, Sucursales sucursales) {}

    public record AdminTurnosPeriodo(String periodo, Map<EstadoTurno, Long> estados) {}
    public record Usuarios(List<UsuariosPeriodo> nuevosPorPeriodo,
                           Map<ERol, Long> activosActualesPorRol) {}
    public record UsuariosPeriodo(String periodo, Map<ERol, Long> altasPorRol) {}
    public record Sucursales(List<SucursalesPeriodo> nuevasPorPeriodo,
                             long activasActualmente, long inactivasActualmente) {}
    public record SucursalesPeriodo(String periodo, long nuevas) {}
}
