package com.gg.turnlook.features.pago;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.gg.turnlook.features.turno.EstadoTurno;
import com.gg.turnlook.features.turno.Turno;
import com.gg.turnlook.features.turno.TurnoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class EstadoPagoPersistenciaTest {

    @Autowired
    private TurnoRepository turnoRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @Transactional
    void actualizaElEstadoSoloSiCoincideElEstadoEsperado() {
        Turno turno = new Turno();
        turno.setFechaHora(LocalDateTime.now().plusDays(1));
        turno.setFechaReserva(LocalDateTime.now());
        turno.setEstado(EstadoTurno.PENDIENTE);
        turnoRepository.saveAndFlush(turno);

        assertEquals(1, turnoRepository.actualizarEstadoSiEs(
                turno.getId(), List.of(EstadoTurno.PENDIENTE), EstadoTurno.CONFIRMADO));
        assertEquals(0, turnoRepository.actualizarEstadoSiEs(
                turno.getId(), List.of(EstadoTurno.PENDIENTE), EstadoTurno.PAGO_RECHAZADO));

        entityManager.clear();
        assertEquals(EstadoTurno.CONFIRMADO,
                turnoRepository.findById(turno.getId()).orElseThrow().getEstado());
    }
}
