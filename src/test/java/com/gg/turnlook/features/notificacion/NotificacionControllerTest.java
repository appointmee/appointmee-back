package com.gg.turnlook.features.notificacion;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gg.turnlook.features.notificacion.dto.ContadorNoLeidasDTO;
import com.gg.turnlook.features.notificacion.dto.NotificacionDTO;
import com.gg.turnlook.features.notificacion.dto.PaginaNotificacionesDTO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class NotificacionControllerTest {

    private NotificacionService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(NotificacionService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificacionController(service))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ana@example.com", null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void devuelveElListadoPaginadoDelUsuarioAutenticado() throws Exception {
        when(service.listar("ana@example.com", false, 1, 5))
                .thenReturn(new PaginaNotificacionesDTO(
                        List.of(new NotificacionDTO(
                                1, TipoNotificacion.RESERVA, "Turno confirmado",
                                "Tu turno quedó confirmado.",
                                LocalDateTime.of(2026, 11, 26, 10, 0), false)),
                        1, 5, 6, 2, 3));

        mockMvc.perform(get("/notificaciones")
                        .param("leida", "false")
                        .param("pagina", "1")
                        .param("tamano", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.tamano").value(5))
                .andExpect(jsonPath("$.totalElementos").value(6))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.noLeidas").value(3))
                .andExpect(jsonPath("$.contenido[0].tipo").value("RESERVA"))
                .andExpect(jsonPath("$.contenido[0].titulo").value("Turno confirmado"))
                .andExpect(jsonPath("$.contenido[0].leida").value(false));

        verify(service).listar("ana@example.com", false, 1, 5);
    }

    @Test
    void aplicaLosValoresPorDefectoDePaginacion() throws Exception {
        when(service.listar("ana@example.com", null, 0, 20))
                .thenReturn(new PaginaNotificacionesDTO(List.of(), 0, 20, 0, 0, 0));

        mockMvc.perform(get("/notificaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamano").value(20));

        verify(service).listar("ana@example.com", null, 0, 20);
    }

    @Test
    void devuelveElContadorDeNoLeidas() throws Exception {
        when(service.contarNoLeidas("ana@example.com")).thenReturn(new ContadorNoLeidasDTO(4));

        mockMvc.perform(get("/notificaciones/no-leidas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noLeidas").value(4));

        verify(service).contarNoLeidas("ana@example.com");
    }
}
