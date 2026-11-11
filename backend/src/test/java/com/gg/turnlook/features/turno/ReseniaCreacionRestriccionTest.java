package com.gg.turnlook.features.turno;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gg.turnlook.common.exception.ConflictException;
import com.gg.turnlook.features.resenia.dto.ReseniaCrearDTO;
import com.gg.turnlook.features.usuario.Usuario;
import com.gg.turnlook.features.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:resenia-restriccion;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false",
    "jwt.secret=local-test-secret-only-32-characters-minimum",
    "resend.api-key=test",
    "resend.from=test@example.com",
    "whatsapp.gateway.url=http://localhost:9999",
    "whatsapp.gateway.api-key=test",
    "mercadopago.access-token=test",
    "cloudinary.cloud-name=test",
    "cloudinary.api-key=test",
    "cloudinary.api-secret=test"
})
class ReseniaCreacionRestriccionTest {

    @Autowired
    private TurnoService turnoService;

    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Usuario crearUsuario() {
        Usuario usuario = new Usuario("Ana", "Perez", UUID.randomUUID() + "@example.com", "pwd");
        usuario.setEmailVerificado(true);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private Turno crearTurno(EstadoTurno estado, Usuario cliente) {
        Turno turno = new Turno();
        turno.setFechaHora(LocalDateTime.now().plusDays(1));
        turno.setFechaReserva(LocalDateTime.now());
        turno.setEstado(estado);
        turno.setCliente(cliente);
        return turnoRepository.saveAndFlush(turno);
    }

    private ReseniaCrearDTO dtoValido() {
        ReseniaCrearDTO dto = new ReseniaCrearDTO();
        dto.setPuntuacion(5);
        dto.setComentario("Comentario valido de prueba");
        return dto;
    }

    @Test
    @Transactional
    void realizadoPermiteCrearResenia() {
        Usuario cliente = crearUsuario();
        Turno turno = crearTurno(EstadoTurno.REALIZADO, cliente);

        assertDoesNotThrow(() -> turnoService.reseniarTurno(turno.getId(), dtoValido(), cliente.getEmail()));
    }

    @Test
    @Transactional
    void canceladoNoPermiteCrearResenia() {
        Usuario cliente = crearUsuario();
        Turno turno = crearTurno(EstadoTurno.CANCELADO, cliente);

        assertThrows(ConflictException.class,
                () -> turnoService.reseniarTurno(turno.getId(), dtoValido(), cliente.getEmail()));
    }

    @Test
    @Transactional
    void otrosEstadosNoPermitenCrearResenia() {
        Usuario cliente = crearUsuario();
        for (EstadoTurno estado : new EstadoTurno[] {
                EstadoTurno.PENDIENTE, EstadoTurno.CONFIRMADO, EstadoTurno.PAGO_RECHAZADO }) {
            Turno turno = crearTurno(estado, cliente);
            assertThrows(ConflictException.class,
                    () -> turnoService.reseniarTurno(turno.getId(), dtoValido(), cliente.getEmail()));
        }
    }
}
