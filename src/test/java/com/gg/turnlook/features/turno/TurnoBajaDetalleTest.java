package com.gg.turnlook.features.turno;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gg.turnlook.features.notificacion.NotificacionEventosService;
import com.gg.turnlook.features.resenia.ReseniaService;
import com.gg.turnlook.features.servicio.Servicio;
import com.gg.turnlook.features.servicio.ServicioService;
import com.gg.turnlook.features.sucursal.Sucursal;
import com.gg.turnlook.features.sucursal.SucursalService;
import com.gg.turnlook.features.sucursal.categoria.Categoria;
import com.gg.turnlook.features.sucursal.categoria.ECategoria;
import com.gg.turnlook.features.turno.dto.TurnoClienteResponseDTO;
import com.gg.turnlook.features.turno.dto.TurnoResponseDTO;
import com.gg.turnlook.features.usuario.Usuario;
import com.gg.turnlook.features.usuario.UsuarioService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** SCRUM-365: el detalle conserva los datos históricos tras una baja lógica. */
class TurnoBajaDetalleTest {

    private TurnoRepository turnoRepo;
    private SucursalService sucursalService;
    private TurnoService service;

    private Sucursal sucursal;
    private Usuario empleado;

    @BeforeEach
    void setUp() {
        turnoRepo = mock(TurnoRepository.class);
        sucursalService = mock(SucursalService.class);
        service = new TurnoService(turnoRepo, mock(UsuarioService.class),
                mock(ServicioService.class), sucursalService,
                mock(ReseniaService.class), mock(NotificacionEventosService.class));

        sucursal = new Sucursal();
        sucursal.setId(4);
        sucursal.setNombre("Sucursal Centro");
        sucursal.setDireccion("Av. Principal 123");
        sucursal.setTelefono("11-5555-5555");
        sucursal.setCategoria(new Categoria(ECategoria.PELUQUERIA));
        sucursal.setActivo(false);

        empleado = new Usuario();
        empleado.setId(9);
        empleado.setNombre("Ana");
        empleado.setApellido("Pérez");
        empleado.setEmail("empleado@example.com");
        empleado.setActivo(false);

        when(sucursalService.empleadoPertenece(4, 9)).thenReturn(false);
        when(turnoRepo.findByIdConDetalles(41)).thenReturn(Optional.of(turno()));
    }

    @Test
    void elEmpleadoDesvinculadoPuedeVerElDetalleConDatosHistoricos() {
        TurnoResponseDTO detalle =
                service.verDetalleTurnoPorSucursal(41, "empleado@example.com");

        assertNotNull(detalle.getSucursal());
        assertEquals(4, detalle.getSucursal().getId());
        assertEquals("Sucursal Centro", detalle.getSucursal().getNombre());
        assertEquals("Av. Principal 123", detalle.getSucursal().getDireccion());
        assertEquals("PELUQUERIA", detalle.getSucursal().getCategoria());
        assertEquals("Ana", detalle.getEmpleado().getNombre());
        assertEquals("empleado@example.com", detalle.getEmpleado().getEmail());
        assertFalse(detalle.isEmpleadoActivo());
        assertFalse(detalle.isEmpleadoPerteneceASucursal());
        assertFalse(detalle.isSucursalActiva());
        assertEquals(EstadoTurno.CANCELADO, detalle.getEstadoTurno());
    }

    @Test
    void elClienteTambienRecibeLosDatosHistoricosYLosEstadosReales() {
        TurnoClienteResponseDTO detalle =
                service.verDetalleTurnoPropio("cliente@example.com", 41);

        assertNotNull(detalle.getSucursal());
        assertEquals(4, detalle.getSucursal().getId());
        assertEquals("Sucursal Centro", detalle.getSucursal().getNombre());
        assertEquals("Ana", detalle.getEmpleado().getNombre());
        assertFalse(detalle.isEmpleadoActivo());
        assertFalse(detalle.isEmpleadoPerteneceASucursal());
        assertFalse(detalle.isSucursalActiva());
        assertEquals("11-5555-5555", detalle.getSucursal().getTelefono());
    }

    private Turno turno() {
        Usuario cliente = new Usuario();
        cliente.setNombre("María");
        cliente.setApellido("Gómez");
        cliente.setEmail("cliente@example.com");

        Servicio servicio = new Servicio();
        servicio.setId(7);
        servicio.setNombre("Corte de pelo");
        servicio.setPrecio(new BigDecimal("1000"));
        servicio.setDuracion(30);
        servicio.setSucursal(sucursal);

        Turno turno = new Turno();
        turno.setId(41);
        turno.setFechaReserva(LocalDateTime.of(2026, 11, 1, 10, 0));
        turno.setFechaHora(LocalDateTime.of(2026, 11, 10, 10, 0));
        turno.setEstado(EstadoTurno.CANCELADO);
        turno.setCliente(cliente);
        turno.setEmpleado(empleado);
        turno.setServicio(servicio);
        return turno;
    }
}
