package com.gg.turnlook.features.solicitud.empleador;

import com.gg.turnlook.features.solicitud.empleador.dto.SolicitudEmpleadorCrearDTO;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(SolicitudEmpleadorPermisosTest.Config.class)
class SolicitudEmpleadorPermisosTest {
    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean SolicitudEmpleadorService servicio() {
            return mock(SolicitudEmpleadorService.class);
        }
        @Bean SolicitudEmpleadorController controller(SolicitudEmpleadorService servicio) {
            return new SolicitudEmpleadorController(servicio);
        }
    }

    @Autowired SolicitudEmpleadorController controller;
    @Autowired SolicitudEmpleadorService servicio;

    @BeforeEach void preparar() { reset(servicio); }
    @AfterEach void limpiar() { SecurityContextHolder.clearContext(); }

    @Test void clienteEmpleadoYEmpleadorPuedenEnviarYConsultarSusPropiasSolicitudes() {
        for (String rol : List.of("CLIENTE", "EMPLEADO", "EMPLEADOR")) {
            sesion(rol);
            String email = rol.toLowerCase() + "@example.com";
            var datos = new SolicitudEmpleadorCrearDTO();
            datos.setMotivo("Quiero solicitar otro negocio para gestionar turnos.");
            assertEquals(200, controller.solicitarSerEmpleador(datos, email).getStatusCode().value());
            assertEquals(200, controller.listarSolicitudesEmpleadorPropias(email).getStatusCode().value());
            verify(servicio).registrarSolicitud(datos, email);
            verify(servicio).listarSolicitudesPropias(email);
        }
    }

    @Test void administradorNoPuedeEnviarNiConsultarSolicitudesComoSolicitante() {
        sesion("ADMINISTRADOR", "EMPLEADOR", "CLIENTE");
        var datos = new SolicitudEmpleadorCrearDTO();
        assertThrows(AccessDeniedException.class, () -> controller.solicitarSerEmpleador(datos, "admin@example.com"));
        assertThrows(AccessDeniedException.class, () -> controller.listarSolicitudesEmpleadorPropias("admin@example.com"));
        verifyNoInteractions(servicio);
    }

    @Test void empleadorNoPuedeListarNiAprobarSolicitudesDeOtrosUsuarios() {
        sesion("EMPLEADOR");
        assertThrows(AccessDeniedException.class, () -> controller.listarSolicitudesEmpleador());
        assertThrows(AccessDeniedException.class, () -> controller.aprobarSolicitudEmpleador(10));
        verifyNoInteractions(servicio);
    }

    private void sesion(String... roles) {
        var authorities = java.util.Arrays.stream(roles)
                .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol)).toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("usuario@example.com", null, authorities));
    }
}
