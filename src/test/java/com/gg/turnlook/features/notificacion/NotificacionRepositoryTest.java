package com.gg.turnlook.features.notificacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.gg.turnlook.features.usuario.Usuario;
import com.gg.turnlook.features.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class NotificacionRepositoryTest {

    @Autowired private NotificacionRepository repository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EntityManager entityManager;

    private static final Pageable ORDEN = PageRequest.of(0, 10);

    @Test
    void persisteTipoTituloMensajeFechaYEstado() {
        Usuario ana = guardarUsuario("ana@example.com");

        Notificacion notificacion = new Notificacion();
        notificacion.setUsuario(ana);
        notificacion.setTipo(TipoNotificacion.RESERVA);
        notificacion.setTitulo("Turno confirmado");
        notificacion.setMensaje("Tu turno quedó confirmado para el viernes.");
        notificacion.setLeida(false);
        Integer id = repository.saveAndFlush(notificacion).getId();

        entityManager.clear();
        Notificacion guardada = repository.findById(id).orElseThrow();

        assertEquals(TipoNotificacion.RESERVA, guardada.getTipo());
        assertEquals("Turno confirmado", guardada.getTitulo());
        assertEquals("Tu turno quedó confirmado para el viernes.", guardada.getMensaje());
        assertNotNull(guardada.getFechaCreacion());
        assertFalse(guardada.isLeida());
        assertEquals(ana.getId(), guardada.getUsuario().getId());
    }

    @Test
    void soloDevuelveNotificacionesDelUsuarioAutenticado() {
        Usuario ana = guardarUsuario("ana@example.com");
        Usuario beto = guardarUsuario("beto@example.com");
        guardarNotificacion(ana, "De Ana", LocalDateTime.now());
        guardarNotificacion(beto, "De Beto", LocalDateTime.now());

        Page<Notificacion> deAna = repository.listarPorUsuario("ana@example.com", null, ORDEN);
        Page<Notificacion> deBeto = repository.listarPorUsuario("beto@example.com", null, ORDEN);

        assertEquals(List.of("De Ana"),
                deAna.getContent().stream().map(Notificacion::getTitulo).toList());
        assertEquals(List.of("De Beto"),
                deBeto.getContent().stream().map(Notificacion::getTitulo).toList());
    }

    @Test
    void filtraPorEstadoDeLectura() {
        Usuario ana = guardarUsuario("ana@example.com");
        guardarNotificacion(ana, "Leída", LocalDateTime.now().minusDays(2));
        Notificacion pendiente = guardarNotificacion(ana, "Pendiente", LocalDateTime.now());

        Page<Notificacion> leidas =
                repository.listarPorUsuario("ana@example.com", true, ORDEN);
        Page<Notificacion> noLeidas =
                repository.listarPorUsuario("ana@example.com", false, ORDEN);
        Page<Notificacion> todas =
                repository.listarPorUsuario("ana@example.com", null, ORDEN);

        assertEquals(List.of("Leída"), leidas.getContent().stream()
                .map(Notificacion::getTitulo).toList());
        assertEquals(List.of("Pendiente"), noLeidas.getContent().stream()
                .map(Notificacion::getTitulo).toList());
        assertEquals(2, todas.getTotalElements());
        assertEquals(pendiente.getId(), noLeidas.getContent().getFirst().getId());
    }

    @Test
    void paginaOrdenadoDeLaMasNuevaALaMasVieja() {
        Usuario ana = guardarUsuario("ana@example.com");
        guardarNotificacion(ana, "Vieja", LocalDateTime.now().minusDays(3));
        guardarNotificacion(ana, "Intermedia", LocalDateTime.now().minusDays(2));
        guardarNotificacion(ana, "Nueva", LocalDateTime.now().minusDays(1));

        Page<Notificacion> primera = repository.listarPorUsuario(
                "ana@example.com", null, PageRequest.of(0, 2));
        Page<Notificacion> segunda = repository.listarPorUsuario(
                "ana@example.com", null, PageRequest.of(1, 2));

        assertEquals(3, primera.getTotalElements());
        assertEquals(2, primera.getTotalPages());
        assertEquals(List.of("Nueva", "Intermedia"), primera.getContent().stream()
                .map(Notificacion::getTitulo).toList());
        assertEquals(List.of("Vieja"), segunda.getContent().stream()
                .map(Notificacion::getTitulo).toList());
    }

    @Test
    void cuentaSoloLasNoLeidasDelUsuario() {
        Usuario ana = guardarUsuario("ana@example.com");
        Usuario beto = guardarUsuario("beto@example.com");
        guardarNotificacion(ana, "Ana pendiente", LocalDateTime.now());
        guardarNotificacion(ana, "Ana leída", LocalDateTime.now());
        guardarNotificacion(beto, "Beto pendiente", LocalDateTime.now());

        assertEquals(1, repository.contarNoLeidas("ana@example.com"));
        assertEquals(1, repository.contarNoLeidas("beto@example.com"));
        assertEquals(0, repository.contarNoLeidas("nadie@example.com"));
    }

    private Usuario guardarUsuario(String email) {
        return usuarioRepository.saveAndFlush(new Usuario("Ana", "Paz", email, "hash"));
    }

    private Notificacion guardarNotificacion(Usuario usuario, String titulo, LocalDateTime fecha) {
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuario(usuario);
        notificacion.setTipo(TipoNotificacion.SISTEMA);
        notificacion.setTitulo(titulo);
        notificacion.setMensaje("Mensaje de " + titulo);
        notificacion.setFechaCreacion(fecha);
        notificacion.setLeida(titulo.contains("leída") || titulo.equals("Leída"));
        return repository.saveAndFlush(notificacion);
    }
}
