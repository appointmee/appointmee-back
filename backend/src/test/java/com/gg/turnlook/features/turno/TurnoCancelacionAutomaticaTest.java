package com.gg.turnlook.features.turno;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:turno-cancel;DB_CLOSE_DELAY=-1",
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
class TurnoCancelacionAutomaticaTest {

    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private TurnoService turnoService;

    @PersistenceContext
    private EntityManager entityManager;

    private Turno crearTurno(EstadoTurno estado, LocalDateTime fechaReserva,
                            LocalDateTime fechaModificacionEstado) {
        Turno turno = new Turno();
        turno.setFechaHora(LocalDateTime.now().plusDays(1));
        turno.setFechaReserva(fechaReserva);
        turno.setEstado(estado);
        turno.setFechaModificacionEstado(fechaModificacionEstado);
        return turnoRepository.saveAndFlush(turno);
    }

    @Test
    @Transactional
    void cancelaPendienteConMasDe2Horas() {
        LocalDateTime ahora = LocalDateTime.now();
        Turno turno = crearTurno(EstadoTurno.PENDIENTE, ahora.minusHours(3), ahora.minusHours(3));

        int cancelados = turnoService.cancelarTurnosPendientesVencidos();

        assertTrue(cancelados >= 1);
        entityManager.clear();
        Turno actualizado = turnoRepository.findById(turno.getId()).orElseThrow();
        assertEquals(EstadoTurno.CANCELADO, actualizado.getEstado());
        assertNotNull(actualizado.getFechaModificacionEstado());
    }

    @Test
    @Transactional
    void noCancelaPendienteConMenosDe2Horas() {
        LocalDateTime ahora = LocalDateTime.now();
        Turno turno = crearTurno(EstadoTurno.PENDIENTE, ahora.minusMinutes(30), ahora.minusMinutes(30));

        int cancelados = turnoService.cancelarTurnosPendientesVencidos();

        assertEquals(0, cancelados);
        entityManager.clear();
        assertEquals(EstadoTurno.PENDIENTE,
                turnoRepository.findById(turno.getId()).orElseThrow().getEstado());
    }

    @Test
    @Transactional
    void cancelaPendienteConExactamente2Horas() {
        LocalDateTime ahora = LocalDateTime.now();
        Turno turno = crearTurno(EstadoTurno.PENDIENTE, ahora.minusHours(2), ahora.minusHours(2));

        int cancelados = turnoService.cancelarTurnosPendientesVencidos();

        assertEquals(1, cancelados);
        entityManager.clear();
        assertEquals(EstadoTurno.CANCELADO,
                turnoRepository.findById(turno.getId()).orElseThrow().getEstado());
    }

    @Test
    @Transactional
    void pendienteAntiguoSinFechaModificacionUsaFechaCreacion() {
        LocalDateTime ahora = LocalDateTime.now();
        Turno turno = crearTurno(EstadoTurno.PENDIENTE, ahora.minusHours(3), null);

        int cancelados = turnoService.cancelarTurnosPendientesVencidos();

        assertEquals(1, cancelados);
        entityManager.clear();
        assertEquals(EstadoTurno.CANCELADO,
                turnoRepository.findById(turno.getId()).orElseThrow().getEstado());
    }

    @Test
    @Transactional
    void noCancelaConfirmadoAunqueSeaAntiguo() {
        LocalDateTime ahora = LocalDateTime.now();
        Turno turno = crearTurno(EstadoTurno.CONFIRMADO, ahora.minusHours(3), ahora.minusHours(3));

        int cancelados = turnoService.cancelarTurnosPendientesVencidos();

        assertEquals(0, cancelados);
        entityManager.clear();
        assertEquals(EstadoTurno.CONFIRMADO,
                turnoRepository.findById(turno.getId()).orElseThrow().getEstado());
    }

    @Test
    @Transactional
    void excluyeOtrosEstados() {
        LocalDateTime ahora = LocalDateTime.now();
        crearTurno(EstadoTurno.CANCELADO, ahora.minusHours(3), ahora.minusHours(3));
        crearTurno(EstadoTurno.REALIZADO, ahora.minusHours(3), ahora.minusHours(3));
        crearTurno(EstadoTurno.PAGO_RECHAZADO, ahora.minusHours(3), ahora.minusHours(3));

        int cancelados = turnoService.cancelarTurnosPendientesVencidos();

        assertEquals(0, cancelados);
    }

    @Test
    @Transactional
    void modificacionesDeOtrosCamposNoReinicianContador() {
        LocalDateTime ahora = LocalDateTime.now();
        Turno turno = crearTurno(EstadoTurno.PENDIENTE, ahora.minusHours(1), ahora.minusHours(1));
        LocalDateTime antes = turno.getFechaModificacionEstado();

        turno.setFechaHora(LocalDateTime.now().plusDays(2));
        turnoRepository.saveAndFlush(turno);
        entityManager.clear();

        assertEquals(antes.truncatedTo(ChronoUnit.MILLIS),
                turnoRepository.findById(turno.getId()).orElseThrow()
                        .getFechaModificacionEstado().truncatedTo(ChronoUnit.MILLIS));
    }
}
