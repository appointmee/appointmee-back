package com.gg.turnlook.features.solicitud.empleador;

import com.gg.turnlook.features.solicitud.empleador.dto.SolicitudEmpleadorCrearDTO;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDeniedException;
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

    @Test void clientesYEmpleadosPuedenEnviarSolicitudYLosClientesConsultarLasPropias() {
        for (String rol : List.of("CLIENTE", "EMPLEADO")) {
            sesion(rol);
            String email = rol.toLowerCase() + "@example.com";
            var datos = new SolicitudEmpleadorCrearDTO();
            datos.setMotivo("Quiero solicitar otro negocio para gestionar turnos.");
            assertEquals(200, controller.solicitarSerEmpleador(datos, email).getStatusCode().value());
            verify(servicio).registrarSolicitud(datos, email);
        }

        sesion("CLIENTE");
        assertEquals(200,
                controller.listarSolicitudesEmpleadorPropias("cliente@example.com").getStatusCode().value());
        verify(servicio).listarSolicitudesPropias("cliente@example.com");
    }

    @Test void empleadorNoPuedeEnviarNiConsultarSolicitudesComoSolicitante() {
        sesion("EMPLEADOR");
        var datos = new SolicitudEmpleadorCrearDTO();
        assertThrows(AuthorizationDeniedException.class,
                () -> controller.solicitarSerEmpleador(datos, "empleador@example.com"));
        assertThrows(AuthorizationDeniedException.class,
                () -> controller.listarSolicitudesEmpleadorPropias("empleador@example.com"));
        verifyNoInteractions(servicio);
    }

    @Test void administradorNoPuedeEnviarNiConsultarSolicitudesComoSolicitante() {
        sesion("ADMINISTRADOR");
        var datos = new SolicitudEmpleadorCrearDTO();
        assertThrows(AuthorizationDeniedException.class,
                () -> controller.solicitarSerEmpleador(datos, "admin@example.com"));
        assertThrows(AuthorizationDeniedException.class,
                () -> controller.listarSolicitudesEmpleadorPropias("admin@example.com"));
        verifyNoInteractions(servicio);
    }

    @Test void empleadorNoPuedeListarNiAprobarSolicitudesDeOtrosUsuarios() {
        sesion("EMPLEADOR");
        assertThrows(AuthorizationDeniedException.class,
                () -> controller.listarSolicitudesEmpleador(null));
        assertThrows(AuthorizationDeniedException.class,
                () -> controller.listarSolicitudesEmpleador("APROBADA"));
        assertThrows(AuthorizationDeniedException.class,
                () -> controller.aprobarSolicitudEmpleador(10));
        verifyNoInteractions(servicio);
    }

    @Test void administradorPuedeListarTodosLosEstadosYOpcionalmenteFiltrar() {
        sesion("ADMINISTRADOR");
        when(servicio.listarSolicitudesAdministrativas(null)).thenReturn(List.of());
        when(servicio.listarSolicitudesAdministrativas("RECHAZADA")).thenReturn(List.of());

        assertEquals(200, controller.listarSolicitudesEmpleador(null).getStatusCode().value());
        assertEquals(200, controller.listarSolicitudesEmpleador("RECHAZADA").getStatusCode().value());

        verify(servicio).listarSolicitudesAdministrativas(null);
        verify(servicio).listarSolicitudesAdministrativas("RECHAZADA");
    }

    private void sesion(String... roles) {
        var authorities = java.util.Arrays.stream(roles)
                .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol)).toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("usuario@example.com", null, authorities));
    }
}
