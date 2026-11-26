package com.gg.turnlook.features.solicitud.empleador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gg.turnlook.features.solicitud.EstadoSolicitud;
import com.gg.turnlook.features.solicitud.empleador.dto.SolicitudEmpleadorMiniDTO;
import com.gg.turnlook.features.usuario.Usuario;
import com.gg.turnlook.features.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class SolicitudEmpleadorUbicacionesPersistenciaTest {

    @Autowired private SolicitudEmpleadorRepository repository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EntityManager entityManager;

    @Test
    void guardaYRecuperaTodasLasUbicacionesConUnSoloAccesoAlRepositorio() {
        Usuario usuario = guardarUsuario("ubicaciones@example.com");
        SolicitudEmpleador solicitud = nuevaSolicitud(usuario, "Motivo de prueba con longitud suficiente.");
        solicitud.agregarUbicacion(new UbicacionSolicitud(
                "Av. Libertador 1234, CABA", -34.6037, -58.3816, "ChIJ_uno"));
        solicitud.agregarUbicacion(new UbicacionSolicitud(
                "Av. Corrientes 2222, CABA", -34.6, -58.4, "ChIJ_dos"));
        Integer id = repository.saveAndFlush(solicitud).getId();

        entityManager.clear();

        SolicitudEmpleador guardada = repository.findByIdConUbicaciones(id).orElseThrow();
        assertEquals(2, guardada.getUbicaciones().size());
        assertEquals("ChIJ_uno", guardada.getUbicaciones().get(0).getGooglePlaceId());
        assertEquals(-34.6, guardada.getUbicaciones().get(1).getLatitud());
        assertEquals("Av. Corrientes 2222, CABA",
                guardada.getUbicaciones().get(1).getDireccion());
        assertEquals(usuario.getId(), guardada.getUsuario().getId());
    }

    @Test
    void persistirSinUbicacionesSigueSiendoValidoParaSolicitudesHistoricas() {
        Usuario usuario = guardarUsuario("historica@example.com");
        SolicitudEmpleador historica = nuevaSolicitud(usuario, "Solicitud historica sin ubicaciones.");
        Integer id = repository.saveAndFlush(historica).getId();
        // @PrePersist fuerza PENDIENTE: el estado historico se asigna despues.
        historica.setEstado(EstadoSolicitud.RECHAZADA);
        repository.flush();

        entityManager.clear();

        SolicitudEmpleador guardada = repository.findByIdConUbicaciones(id).orElseThrow();
        assertTrue(guardada.getUbicaciones().isEmpty());
        assertEquals(EstadoSolicitud.RECHAZADA, guardada.getEstado());
    }

    @Test
    void conservaComentariosDe3000Caracteres() {
        Usuario usuario = guardarUsuario("comentario@example.com");
        String motivo = "M".repeat(3000);
        Integer id = repository.saveAndFlush(nuevaSolicitud(usuario, motivo)).getId();

        entityManager.clear();

        assertEquals(3000, repository.findById(id).orElseThrow().getMotivo().length());
    }

    @Test
    void filtroAdministrativoSeAplicaEnLaConsultaYDevuelveLosTresEstados() {
        Usuario usuario = guardarUsuario("listado@example.com");
        guardarSolicitud(usuario, "Solicitud pendiente de prueba.",
                LocalDate.of(2026, 11, 20), EstadoSolicitud.PENDIENTE);
        SolicitudEmpleador aprobada = guardarSolicitud(usuario, "Solicitud aprobada de prueba.",
                LocalDate.of(2026, 11, 21), EstadoSolicitud.APROBADA);
        guardarSolicitud(usuario, "Solicitud rechazada de prueba.",
                LocalDate.of(2026, 11, 22), EstadoSolicitud.RECHAZADA);
        entityManager.clear();

        Page<SolicitudEmpleadorMiniDTO> todas = repository.findMiniAdmin(null, PageRequest.of(0, 10));
        Page<SolicitudEmpleadorMiniDTO> aprobadas = repository.findMiniAdmin(
                EstadoSolicitud.APROBADA, PageRequest.of(0, 10));

        assertEquals(3, todas.getTotalElements());
        assertEquals(1, todas.getTotalPages());
        assertEquals(List.of(EstadoSolicitud.RECHAZADA, EstadoSolicitud.APROBADA,
                        EstadoSolicitud.PENDIENTE),
                todas.getContent().stream().map(SolicitudEmpleadorMiniDTO::getEstado).toList());
        assertEquals(1, aprobadas.getTotalElements());
        assertEquals(List.of(aprobada.getId()),
                aprobadas.getContent().stream().map(SolicitudEmpleadorMiniDTO::getId).toList());
    }

    @Test
    void primeraPaginaSinFiltroTraeSoloLosRegistrosDeLaPagina() {
        List<Integer> id = cargarDatosPaginados();

        Page<SolicitudEmpleadorMiniDTO> pagina =
                repository.findMiniAdmin(null, PageRequest.of(0, 2));

        assertEquals(2, pagina.getContent().size());
        assertEquals(5, pagina.getTotalElements());
        assertEquals(3, pagina.getTotalPages());
        assertEquals(0, pagina.getNumber());
        assertEquals(2, pagina.getSize());
        assertEquals(List.of(id.get(4), id.get(3)), ids(pagina));
    }

    @Test
    void paginaIntermediaYUltimaSinFiltroMantienenOrdenEstable() {
        List<Integer> id = cargarDatosPaginados();

        Page<SolicitudEmpleadorMiniDTO> primera =
                repository.findMiniAdmin(null, PageRequest.of(0, 2));
        Page<SolicitudEmpleadorMiniDTO> intermedia =
                repository.findMiniAdmin(null, PageRequest.of(1, 2));
        Page<SolicitudEmpleadorMiniDTO> ultima =
                repository.findMiniAdmin(null, PageRequest.of(2, 2));

        assertEquals(List.of(id.get(2), id.get(1)), ids(intermedia));
        assertEquals(List.of(id.get(0)), ids(ultima));
        assertEquals(1, intermedia.getNumber());
        assertEquals(3, ultima.getTotalPages());

        // Sin duplicados ni huecos al concatenar las tres paginas.
        List<Integer> todos = new ArrayList<>(ids(primera));
        todos.addAll(ids(intermedia));
        todos.addAll(ids(ultima));
        assertEquals(List.of(id.get(4), id.get(3), id.get(2), id.get(1), id.get(0)), todos);
    }

    @Test
    void filtroPorCadaEstadoRespetaElTotalYElOrden() {
        List<Integer> id = cargarDatosPaginados();

        Page<SolicitudEmpleadorMiniDTO> pendientes =
                repository.findMiniAdmin(EstadoSolicitud.PENDIENTE, PageRequest.of(0, 10));
        Page<SolicitudEmpleadorMiniDTO> aprobadas =
                repository.findMiniAdmin(EstadoSolicitud.APROBADA, PageRequest.of(0, 10));
        Page<SolicitudEmpleadorMiniDTO> rechazadas =
                repository.findMiniAdmin(EstadoSolicitud.RECHAZADA, PageRequest.of(0, 10));

        assertEquals(2, pendientes.getTotalElements());
        assertEquals(List.of(id.get(4), id.get(0)), ids(pendientes));
        assertEquals(2, aprobadas.getTotalElements());
        assertEquals(List.of(id.get(3), id.get(1)), ids(aprobadas));
        assertEquals(1, rechazadas.getTotalElements());
        assertEquals(List.of(id.get(2)), ids(rechazadas));
    }

    @Test
    void combinacionDeFiltroYPaginacionCuentaSoloLosRegistrosFiltrados() {
        List<Integer> id = cargarDatosPaginados();

        Page<SolicitudEmpleadorMiniDTO> pagina = repository.findMiniAdmin(
                EstadoSolicitud.APROBADA, PageRequest.of(1, 1));

        assertEquals(1, pagina.getContent().size());
        assertEquals(2, pagina.getTotalElements());
        assertEquals(2, pagina.getTotalPages());
        assertEquals(1, pagina.getNumber());
        assertEquals(List.of(id.get(1)), ids(pagina));
    }

    @Test
    void paginaSinResultadosDevuelveMetadatosCorrectos() {
        cargarDatosPaginados();

        Page<SolicitudEmpleadorMiniDTO> pagina = repository.findMiniAdmin(
                EstadoSolicitud.RECHAZADA, PageRequest.of(3, 1));

        assertTrue(pagina.getContent().isEmpty());
        assertEquals(1, pagina.getTotalElements());
        assertEquals(1, pagina.getTotalPages());
        assertEquals(3, pagina.getNumber());
        assertEquals(1, pagina.getSize());
    }

    /// Cinco solicitudes ordenadas por fecha descendente, con dos compartiendo
    /// la misma fecha para verificar el desempate por ID. Devuelve los IDs en
    /// orden de creacion (uno, dos, tres, cuatro, cinco).
    private List<Integer> cargarDatosPaginados() {
        Usuario usuario = guardarUsuario("paginacion@example.com");
        List<Integer> id = new ArrayList<>();
        id.add(guardarSolicitud(usuario, "Solicitud uno con motivo suficiente.",
                LocalDate.of(2026, 11, 20), EstadoSolicitud.PENDIENTE).getId());
        id.add(guardarSolicitud(usuario, "Solicitud dos con motivo suficiente.",
                LocalDate.of(2026, 11, 21), EstadoSolicitud.APROBADA).getId());
        id.add(guardarSolicitud(usuario, "Solicitud tres con motivo suficiente.",
                LocalDate.of(2026, 11, 22), EstadoSolicitud.RECHAZADA).getId());
        id.add(guardarSolicitud(usuario, "Solicitud cuatro con motivo suficiente.",
                LocalDate.of(2026, 11, 22), EstadoSolicitud.APROBADA).getId());
        id.add(guardarSolicitud(usuario, "Solicitud cinco con motivo suficiente.",
                LocalDate.of(2026, 11, 23), EstadoSolicitud.PENDIENTE).getId());
        entityManager.clear();
        return id;
    }

    private List<Integer> ids(Page<SolicitudEmpleadorMiniDTO> pagina) {
        return pagina.getContent().stream().map(SolicitudEmpleadorMiniDTO::getId).toList();
    }

    private Usuario guardarUsuario(String email) {
        return usuarioRepository.saveAndFlush(
                new Usuario("Ana", "Gomez", email, "hash"));
    }

    private SolicitudEmpleador nuevaSolicitud(Usuario usuario, String motivo) {
        return new SolicitudEmpleador(motivo, usuario);
    }

    /// @PrePersist fuerza PENDIENTE y la fecha actual: se guarda primero y
    /// despues se ajustan estado y fecha para simular solicitudes historicas.
    private SolicitudEmpleador guardarSolicitud(Usuario usuario, String motivo,
                                                LocalDate fecha, EstadoSolicitud estado) {
        SolicitudEmpleador solicitud = repository.saveAndFlush(nuevaSolicitud(usuario, motivo));
        solicitud.setEstado(estado);
        solicitud.setFechaSolicitud(fecha);
        return repository.saveAndFlush(solicitud);
    }

}
