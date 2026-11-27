package com.gg.turnlook.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    @Test
    void elErrorGeneralNoExponeElMensajeDeLaExcepcion() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<?> respuesta = handler.handleGeneral(
                new IllegalStateException("Authorization: Bearer SUPER_SECRETO"));

        assertEquals(500, respuesta.getStatusCode().value());
        String cuerpo = String.valueOf(respuesta.getBody());
        assertFalse(cuerpo.contains("SUPER_SECRETO"));
        assertEquals("Error interno del servidor", cuerpo);
    }
}
