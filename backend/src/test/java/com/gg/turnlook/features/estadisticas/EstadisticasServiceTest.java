package com.gg.turnlook.features.estadisticas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gg.turnlook.common.exception.BadRequestException;
import com.gg.turnlook.features.sucursal.Sucursal;
import com.gg.turnlook.features.sucursal.SucursalRepository;
import com.gg.turnlook.features.sucursal.SucursalService;
import com.gg.turnlook.features.turno.EstadoTurno;
import com.gg.turnlook.features.turno.TurnoRepository;
import com.gg.turnlook.features.usuario.UsuarioRepository;
import com.gg.turnlook.features.usuario.rol.ERol;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EstadisticasServiceTest {
    private TurnoRepository turnoRepository;
    private SucursalService sucursalService;
    private UsuarioRepository usuarioRepository;
    private SucursalRepository sucursalRepository;
    private EstadisticasService service;

    @BeforeEach
    void setUp() {
        turnoRepository = mock(TurnoRepository.class);
        sucursalService = mock(SucursalService.class);
        usuarioRepository = mock(UsuarioRepository.class);
        sucursalRepository = mock(SucursalRepository.class);
        service = new EstadisticasService(turnoRepository, sucursalService,
                usuarioRepository, sucursalRepository);
        Sucursal sucursal = new Sucursal();
        when(sucursalService.listarSucursalPorId(4)).thenReturn(sucursal);
        when(sucursalService.esEmpleadorAca(sucursal, "empleador@test.com")).thenReturn(true);
    }

    @Test
    void agregaTurnosServiciosYCancelacionesPorPeriodo() {
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 1, 31);
        when(turnoRepository.findDatosEstadisticas(4, desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay()))
                .thenReturn(List.of(
                        fila(desde.atTime(10, 0), EstadoTurno.CANCELADO, 8, "Corte"),
                        fila(desde.atTime(11, 0), EstadoTurno.REALIZADO, 8, "Corte"),
                        fila(desde.plusDays(1).atTime(10, 0), EstadoTurno.CONFIRMADO, 9, "Color")));

        EstadisticasResponseDTO respuesta = service.obtener(4, "empleador@test.com", desde, hasta, "DIA");

        assertEquals(3, respuesta.getTurnosPorPeriodo().stream()
                .mapToLong(p -> p.estados().values().stream().mapToLong(Long::longValue).sum()).sum());
        assertEquals(2, respuesta.getDemandaPorServicio().stream()
                .filter(s -> s.servicioId() == 8).findFirst().orElseThrow().cantidad());
        assertEquals(1, respuesta.getCancelaciones().cantidad());
        assertEquals(2, respuesta.getCancelaciones().turnosConcluidos());
        assertEquals("50.00", respuesta.getCancelaciones().tasa().toPlainString());
        assertFalse(respuesta.getIngresos().disponible());
        assertFalse(respuesta.getOcupacion().disponible());
        assertFalse(respuesta.getAusencias().disponible());
    }

    @Test
    void rechazaRangosInvalidos() {
        LocalDate hoy = LocalDate.of(2026, 1, 1);
        assertThrows(BadRequestException.class,
                () -> service.obtener(4, "empleador@test.com", hoy, hoy.minusDays(1), "DIA"));
        assertThrows(BadRequestException.class,
                () -> service.obtener(4, "empleador@test.com", hoy, hoy.plusDays(366), "DIA"));
    }

    @Test
    void agregaLosTresGraficosGlobalesParaAdmin() {
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 1, 31);
        when(turnoRepository.findDatosEstadisticasGlobales(
                desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay()))
                .thenReturn(java.util.Collections.singletonList(
                        new Object[]{desde.atTime(10, 0), EstadoTurno.REALIZADO}));
        when(usuarioRepository.findAltasPorFechaYRol(desde, hasta,
                List.of(ERol.CLIENTE, ERol.EMPLEADOR, ERol.EMPLEADO)))
                .thenReturn(java.util.Collections.singletonList(new Object[]{desde, ERol.CLIENTE}));
        when(usuarioRepository.contarUsuariosActivosPorRol(
                List.of(ERol.CLIENTE, ERol.EMPLEADOR, ERol.EMPLEADO)))
                .thenReturn(java.util.Collections.singletonList(new Object[]{ERol.CLIENTE, 12L}));
        when(sucursalRepository.findAltasPorFecha(desde, hasta))
                .thenReturn(java.util.Collections.singletonList(new Object[]{desde, true}));
        when(sucursalRepository.contarSucursalesPorEstado())
                .thenReturn(java.util.Arrays.asList(new Object[]{true, 5L}, new Object[]{false, 2L}));

        EstadisticasResponseDTO.Admin respuesta = service.obtenerParaAdmin(desde, hasta, "DIA");

        assertEquals(1, respuesta.turnosPorPeriodo().getFirst().estados().get(EstadoTurno.REALIZADO));
        assertEquals(1, respuesta.usuarios().nuevosPorPeriodo().getFirst().altasPorRol().get(ERol.CLIENTE));
        assertEquals(12, respuesta.usuarios().activosActualesPorRol().get(ERol.CLIENTE));
        assertEquals(1, respuesta.sucursales().nuevasPorPeriodo().getFirst().nuevas());
        assertEquals(5, respuesta.sucursales().activasActualmente());
        assertEquals(2, respuesta.sucursales().inactivasActualmente());
    }

    private static Object[] fila(LocalDateTime fecha, EstadoTurno estado, Integer servicioId, String servicio) {
        return new Object[]{fecha, estado, servicioId, servicio};
    }
}
