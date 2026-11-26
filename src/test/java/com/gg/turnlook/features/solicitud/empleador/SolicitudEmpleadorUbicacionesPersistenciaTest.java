package com.gg.turnlook.features.solicitud.empleador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gg.turnlook.features.solicitud.EstadoSolicitud;
import com.gg.turnlook.features.solicitud.empleador.dto.SolicitudEmpleadorMiniDTO;
import com.gg.turnlook.features.usuario.Usuario;
import com.gg.turnlook.features.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
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
        SolicitudEmpleador pendiente = repository.saveAndFlush(
                nuevaSolicitud(usuario, "Solicitud pendiente de prueba."));
        SolicitudEmpleador aprobada = nuevaSolicitud(usuario, "Solicitud aprobada de prueba.");
        repository.saveAndFlush(aprobada);
        aprobada.setEstado(EstadoSolicitud.APROBADA);
        SolicitudEmpleador rechazada = nuevaSolicitud(usuario, "Solicitud rechazada de prueba.");
        repository.saveAndFlush(rechazada);
        rechazada.setEstado(EstadoSolicitud.RECHAZADA);
        repository.flush();
        entityManager.clear();

        List<SolicitudEmpleadorMiniDTO> todas = repository.findMiniAdmin(null, PageRequest.of(0, 10));
        List<SolicitudEmpleadorMiniDTO> aprobadas = repository.findMiniAdmin(
                EstadoSolicitud.APROBADA, PageRequest.of(0, 10));

        assertEquals(3, todas.size());
        assertEquals(Set.of(EstadoSolicitud.PENDIENTE, EstadoSolicitud.APROBADA,
                        EstadoSolicitud.RECHAZADA),
                todas.stream().map(SolicitudEmpleadorMiniDTO::getEstado)
                        .collect(Collectors.toSet()));
        assertEquals(List.of(aprobada.getId()),
                aprobadas.stream().map(SolicitudEmpleadorMiniDTO::getId).toList());
        assertTrue(todas.stream().anyMatch(d -> d.getId().equals(pendiente.getId())));
    }

    private Usuario guardarUsuario(String email) {
        return usuarioRepository.saveAndFlush(
                new Usuario("Ana", "Gomez", email, "hash"));
    }

    private SolicitudEmpleador nuevaSolicitud(Usuario usuario, String motivo) {
        return new SolicitudEmpleador(motivo, usuario);
    }

}
