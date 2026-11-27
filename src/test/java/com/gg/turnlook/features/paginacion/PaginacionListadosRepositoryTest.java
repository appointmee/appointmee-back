package com.gg.turnlook.features.paginacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gg.turnlook.features.resenia.Resenia;
import com.gg.turnlook.features.resenia.ReseniaRepository;
import com.gg.turnlook.features.servicio.Servicio;
import com.gg.turnlook.features.solicitud.EstadoSolicitud;
import com.gg.turnlook.features.solicitud.empleado.SolicitudEmpleado;
import com.gg.turnlook.features.solicitud.empleado.SolicitudEmpleadoRepository;
import com.gg.turnlook.features.solicitud.empleador.SolicitudEmpleador;
import com.gg.turnlook.features.solicitud.empleador.SolicitudEmpleadorRepository;
import com.gg.turnlook.features.sucursal.Sucursal;
import com.gg.turnlook.features.sucursal.SucursalRepository;
import com.gg.turnlook.features.sucursal.categoria.Categoria;
import com.gg.turnlook.features.sucursal.categoria.ECategoria;
import com.gg.turnlook.features.turno.EstadoTurno;
import com.gg.turnlook.features.turno.Turno;
import com.gg.turnlook.features.turno.TurnoRepository;
import com.gg.turnlook.features.usuario.Usuario;
import com.gg.turnlook.features.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/** Verifica que OFFSET/LIMIT y los totales se resuelven en la base. */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class PaginacionListadosRepositoryTest {

    @Autowired private EntityManager entityManager;
    @Autowired private ReseniaRepository reseniaRepo;
    @Autowired private UsuarioRepository usuarioRepo;
    @Autowired private TurnoRepository turnoRepo;
    @Autowired private SolicitudEmpleadorRepository solicitudEmpleadorRepo;
    @Autowired private SolicitudEmpleadoRepository solicitudEmpleadoRepo;
    @Autowired private SucursalRepository sucursalRepo;

    @Test
    void reseniasPaginadasEnBaseIncluyenPrimeraIntermediaYUltima() {
        Sucursal sucursal = sucursal("Reseñas");
        for (int i = 0; i < 12; i++) {
            guardarResenia(sucursal, LocalDate.now().minusDays(i));
        }
        refrescar();

        Page<?> primera = reseniaRepo.findPageReseniasPorSucursal(
                sucursal.getId(), PageRequest.of(0, 10));
        Page<?> ultima = reseniaRepo.findPageReseniasPorSucursal(
                sucursal.getId(), PageRequest.of(1, 10));

        assertEquals(10, primera.getContent().size());
        assertEquals(2, ultima.getContent().size());
        assertEquals(12, primera.getTotalElements());
        assertEquals(2, primera.getTotalPages());
        assertEquals(1, ultima.getNumber());
    }

    @Test
    void usuariosPaginadosCuentanSoloLosQueCoincidenConElFiltro() {
        for (int i = 0; i < 12; i++) {
            guardarUsuario("usuario-" + i + "@test.com", true,
                    i == 0 ? "Sofia" : "Ana", i == 0 ? "Lopez" : "Gomez");
        }
        guardarUsuario("inactivo@test.com", false);
        refrescar();

        Page<Object[]> activos = usuarioRepo.buscarMiniAdminPagina(
                null, null, true, PageRequest.of(0, 10));
        Page<Object[]> segunda = usuarioRepo.buscarMiniAdminPagina(
                null, null, true, PageRequest.of(1, 10));
        Page<Object[]> nombre = usuarioRepo.buscarMiniAdminPagina(
                "sofia", "lopez", true, PageRequest.of(0, 10));

        assertEquals(10, activos.getContent().size());
        assertEquals(2, segunda.getContent().size());
        assertEquals(12, activos.getTotalElements());
        assertEquals(2, activos.getTotalPages());
        assertEquals(1, nombre.getTotalElements());
    }

    @Test
    void misTurnosPaginadosEnBaseConOrdenEstable() {
        Sucursal sucursal = sucursal("Turnos propios");
        Usuario cliente = guardarUsuario("cliente@test.com", true);
        Servicio servicio = servicio(sucursal);
        for (int i = 0; i < 12; i++) {
            guardarTurno(servicio, cliente, null, EstadoTurno.CONFIRMADO,
                    LocalDateTime.now().minusHours(i));
        }
        refrescar();

        Page<?> primera = turnoRepo.findPageByClienteEmail(
                cliente.getEmail(), PageRequest.of(0, 10));
        Page<?> ultima = turnoRepo.findPageByClienteEmail(
                cliente.getEmail(), PageRequest.of(1, 10));

        assertEquals(10, primera.getContent().size());
        assertEquals(2, ultima.getContent().size());
        assertEquals(12, primera.getTotalElements());
    }

    @Test
    void historialYTurnosAsignadosCuentanSoloElEstadoSolicitado() {
        Sucursal sucursal = sucursal("Historial");
        Usuario cliente = guardarUsuario("cliente-historial@test.com", true);
        Usuario empleado = guardarUsuario("empleado-historial@test.com", true);
        Servicio servicio = servicio(sucursal);
        for (int i = 0; i < 12; i++) {
            guardarTurno(servicio, cliente, empleado, EstadoTurno.CONFIRMADO,
                    LocalDateTime.now().minusHours(i));
        }
        guardarTurno(servicio, cliente, empleado, EstadoTurno.CANCELADO,
                LocalDateTime.now().minusHours(30));
        refrescar();

        Page<?> historial = turnoRepo.findPageByServicioSucursalIdAndEstado(
                sucursal.getId(), EstadoTurno.CONFIRMADO, PageRequest.of(1, 10));
        Page<?> asignados = turnoRepo.findPageByEmpleadoEmailAndServicioSucursalIdAndEstado(
                empleado.getEmail(), sucursal.getId(), EstadoTurno.CONFIRMADO,
                PageRequest.of(0, 10));

        assertEquals(2, historial.getContent().size());
        assertEquals(12, historial.getTotalElements());
        assertEquals(10, asignados.getContent().size());
        assertEquals(12, asignados.getTotalElements());
    }

    @Test
    void solicitudesPropiasDelEmpleadorSePaginanEnBase() {
        Usuario usuario = guardarUsuario("solicitante@test.com", true);
        for (int i = 0; i < 12; i++) {
            guardarSolicitudEmpleador(usuario, LocalDate.now().minusDays(i));
        }
        refrescar();

        Page<?> primera = solicitudEmpleadorRepo.findPageByUsuarioEmail(
                usuario.getEmail(), PageRequest.of(0, 10));
        Page<?> ultima = solicitudEmpleadorRepo.findPageByUsuarioEmail(
                usuario.getEmail(), PageRequest.of(1, 10));

        assertEquals(10, primera.getContent().size());
        assertEquals(2, ultima.getContent().size());
        assertEquals(12, primera.getTotalElements());
    }

    @Test
    void solicitudesRecibidasExcluyenSucursalesBajasDelConteo() {
        Sucursal activa = sucursal("Solicitudes activas");
        Sucursal baja = sucursal("Solicitudes bajas");
        baja.setActivo(false);
        sucursalRepo.saveAndFlush(baja);
        Usuario empleado = guardarUsuario("empleado-solicitudes@test.com", true);
        for (int i = 0; i < 12; i++) {
            guardarSolicitudEmpleado(activa, empleado, LocalDate.now().minusDays(i));
        }
        guardarSolicitudEmpleado(baja, empleado, LocalDate.now());
        refrescar();

        Page<?> primera = solicitudEmpleadoRepo.findPageByEmpleadoEmail(
                empleado.getEmail(), PageRequest.of(0, 10));
        Page<?> ultima = solicitudEmpleadoRepo.findPageByEmpleadoEmail(
                empleado.getEmail(), PageRequest.of(1, 10));

        assertEquals(10, primera.getContent().size());
        assertEquals(2, ultima.getContent().size());
        assertEquals(12, primera.getTotalElements());
    }

    @Test
    void invitacionesEnviadasPorSucursalSePaginanEnBase() {
        Sucursal activa = sucursal("Invitaciones activas");
        Sucursal baja = sucursal("Invitaciones bajas");
        baja.setActivo(false);
        sucursalRepo.saveAndFlush(baja);
        Usuario empleado = guardarUsuario("invitado-sucursal@test.com", true);
        for (int i = 0; i < 12; i++) {
            guardarSolicitudEmpleado(activa, empleado, LocalDate.now().minusDays(i));
        }
        guardarSolicitudEmpleado(baja, empleado, LocalDate.now());
        refrescar();

        Page<?> primera = solicitudEmpleadoRepo.findPageMiniBySucursalId(
                activa.getId(), PageRequest.of(0, 10));
        Page<?> ultima = solicitudEmpleadoRepo.findPageMiniBySucursalId(
                activa.getId(), PageRequest.of(1, 10));
        Page<?> vistaPrevia = solicitudEmpleadoRepo.findPageMiniBySucursalId(
                activa.getId(), PageRequest.of(0, 3));

        assertEquals(10, primera.getContent().size());
        assertEquals(2, ultima.getContent().size());
        assertEquals(12, primera.getTotalElements());
        assertEquals(2, primera.getTotalPages());
        assertEquals(3, vistaPrevia.getContent().size());
    }

    @Test
    void busquedaAdminDeSucursalesFiltraPorNombreCategoriaEIdEnLaBase() {
        Sucursal una = sucursal("Central Spa");
        servicio(una);
        una.getEmpleados().add(guardarUsuario("empleado-central@test.com", true));
        sucursalRepo.saveAndFlush(una);

        Sucursal otra = sucursal("Sucursal Norte");
        servicio(otra);
        otra.getEmpleados().add(guardarUsuario("empleado-norte@test.com", true));
        sucursalRepo.saveAndFlush(otra);
        refrescar();

        assertEquals(2, sucursalRepo.contarOperativasParaAdmin(null));
        assertEquals(1, sucursalRepo.contarOperativasParaAdmin("central"));
        assertEquals(2, sucursalRepo.contarOperativasParaAdmin("spa"));
        assertEquals(1, sucursalRepo.contarOperativasParaAdmin(String.valueOf(una.getId())));
        assertEquals(2, sucursalRepo.findIdsOperativasParaAdmin(
                null, PageRequest.of(0, 10)).size());
        assertEquals(1, sucursalRepo.findIdsOperativasParaAdmin(
                "norte", PageRequest.of(0, 10)).size());
    }

    @Test
    void listadosVaciosDevuelvenMetadatosSinContenido() {
        Sucursal sucursal = sucursal("Sin datos");

        Page<?> resenias = reseniaRepo.findPageReseniasPorSucursal(
                sucursal.getId(), PageRequest.of(0, 10));
        Page<?> turnos = turnoRepo.findPageByClienteEmail(
                "nadie@test.com", PageRequest.of(0, 10));

        assertTrue(resenias.getContent().isEmpty());
        assertEquals(0, resenias.getTotalElements());
        assertEquals(0, resenias.getTotalPages());
        assertTrue(turnos.getContent().isEmpty());
        assertEquals(0, turnos.getTotalElements());
    }

    private Usuario guardarUsuario(String email, boolean activo) {
        return guardarUsuario(email, activo, "Ana", "Gomez");
    }

    private Usuario guardarUsuario(String email, boolean activo,
                                   String nombre, String apellido) {
        Usuario usuario = new Usuario(nombre, apellido, email, "hash");
        usuario.setActivo(activo);
        entityManager.persist(usuario);
        entityManager.flush();
        return usuario;
    }

    private Sucursal sucursal(String nombre) {
        Categoria categoria = new Categoria(ECategoria.SPA);
        entityManager.persist(categoria);
        Sucursal sucursal = new Sucursal();
        sucursal.setNombre(nombre);
        sucursal.setDireccion("Dirección " + nombre);
        sucursal.setDescripcion("Sucursal de prueba");
        sucursal.setHoraApertura(LocalTime.of(9, 0));
        sucursal.setHoraCierre(LocalTime.of(18, 0));
        sucursal.setActivo(true);
        sucursal.setCategoria(categoria);
        return sucursalRepo.saveAndFlush(sucursal);
    }

    private Servicio servicio(Sucursal sucursal) {
        Servicio servicio = new Servicio(
                "Servicio", "Descripción", 30, new BigDecimal("1000"), sucursal);
        servicio.setActivo(true);
        entityManager.persist(servicio);
        entityManager.flush();
        return servicio;
    }

    private Turno guardarTurno(Servicio servicio, Usuario cliente, Usuario empleado,
                               EstadoTurno estado, LocalDateTime fechaHora) {
        Turno turno = new Turno(fechaHora, cliente, empleado, servicio);
        turno.setFechaReserva(fechaHora.minusDays(1));
        turno.setEstado(estado);
        entityManager.persist(turno);
        entityManager.flush();
        return turno;
    }

    private void guardarResenia(Sucursal sucursal, LocalDate fecha) {
        Usuario cliente = guardarUsuario(
                "cliente-resenia-" + System.nanoTime() + "@test.com", true);
        Servicio servicio = servicio(sucursal);
        Turno turno = guardarTurnoTemporal(servicio, cliente);
        Resenia resenia = new Resenia(5, "Comentario", turno);
        resenia.setFechaResenia(fecha);
        entityManager.persist(resenia);
        entityManager.flush();
    }

    private Turno guardarTurnoTemporal(Servicio servicio, Usuario cliente) {
        return guardarTurno(servicio, cliente, null, EstadoTurno.REALIZADO,
                LocalDateTime.now().minusDays(2));
    }

    private void guardarSolicitudEmpleador(Usuario usuario, LocalDate fecha) {
        SolicitudEmpleador solicitud = new SolicitudEmpleador(
                "Motivo de prueba con longitud suficiente.", usuario);
        solicitud = solicitudEmpleadorRepo.saveAndFlush(solicitud);
        solicitud.setFechaSolicitud(fecha);
        solicitudEmpleadorRepo.saveAndFlush(solicitud);
    }

    private void guardarSolicitudEmpleado(Sucursal sucursal, Usuario empleado,
                                          LocalDate fecha) {
        SolicitudEmpleado solicitud = new SolicitudEmpleado(sucursal, empleado);
        solicitud = solicitudEmpleadoRepo.saveAndFlush(solicitud);
        solicitud.setFechaSolicitud(fecha);
        solicitudEmpleadoRepo.saveAndFlush(solicitud);
    }

    private void refrescar() {
        entityManager.flush();
        entityManager.clear();
    }
}
