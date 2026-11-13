package com.gg.turnlook.features.observabilidad;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ObservabilidadControllerTest {

    @Test
    void aceptaReportesDeRutasNoEncontradas() throws Exception {
        var mockMvc = MockMvcBuilders.standaloneSetup(
                new ObservabilidadController(new ObjectMapper())
        ).build();

        mockMvc.perform(post("/observabilidad/404")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"path":"/ruta-inexistente","referrer":"https://example.com/inicio?origen=prueba"}
                                """))
                .andExpect(status().isAccepted());
    }
}
