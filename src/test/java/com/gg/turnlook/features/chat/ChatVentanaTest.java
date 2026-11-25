package com.gg.turnlook.features.chat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gg.turnlook.common.exception.ForbiddenException;
import com.gg.turnlook.features.turno.EstadoTurno;
import com.gg.turnlook.features.turno.Turno;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Ventana de interacción: durante el turno y hasta 3 días después de finalizado. */
class ChatVentanaTest {

    private final ConversacionRepository conversacionRepo = mock(ConversacionRepository.class);
    private final ConversacionParticipanteRepository participanteRepo =
            mock(ConversacionParticipanteRepository.class);
    private final Conversacion conversacion = mock(Conversacion.class);
    private final Turno turno = new Turno();

    private ChatAccesoService acceso;

    @BeforeEach
    void setUp() {
        acceso = new ChatAccesoService(conversacionRepo, participanteRepo);
        when(conversacion.getTurno()).thenReturn(turno);
        turno.setEstado(EstadoTurno.CONFIRMADO);
        turno.setFechaHora(LocalDateTime.now().minusDays(5));
    }

    @Test
    void turnoActivoPermiteInteraccion() {
        assertTrue(acceso.ventanaAbierta(conversacion));
    }

    @Test
    void finalizadoHaceDosDiasPermiteInteraccion() {
        turno.setEstado(EstadoTurno.REALIZADO);
        turno.setFechaModificacionEstado(LocalDateTime.now().minusDays(2));

        assertTrue(acceso.ventanaAbierta(conversacion));
    }

    @Test
    void finalizadoHaceCuatroDiasCierraLaInteraccion() {
        turno.setEstado(EstadoTurno.REALIZADO);
        turno.setFechaModificacionEstado(LocalDateTime.now().minusDays(4));

        assertFalse(acceso.ventanaAbierta(conversacion));
        assertThrows(ForbiddenException.class, () -> acceso.validarVentana(conversacion));
    }

    @Test
    void sinFechaDeFinalizacionUsaLaFechaDelTurno() {
        turno.setEstado(EstadoTurno.REALIZADO);
        turno.setFechaModificacionEstado(null);
        turno.setFechaHora(LocalDateTime.now().minusDays(4));

        assertFalse(acceso.ventanaAbierta(conversacion));
    }

    @Test
    void turnoCanceladoCierraLaInteraccion() {
        turno.setEstado(EstadoTurno.CANCELADO);

        assertFalse(acceso.ventanaAbierta(conversacion));
        assertThrows(ForbiddenException.class, () -> acceso.validarVentana(turno));
    }

    @Test
    void conversacionSinTurnoNoVence() {
        when(conversacion.getTurno()).thenReturn(null);

        assertTrue(acceso.ventanaAbierta(conversacion));
    }
}
