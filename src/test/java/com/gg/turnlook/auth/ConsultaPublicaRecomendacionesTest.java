package com.gg.turnlook.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gg.turnlook.features.resenia.ReseniaService;
import com.gg.turnlook.features.sucursal.SucursalDescubrimientoService;
import com.gg.turnlook.features.sucursal.SucursalService;
import com.gg.turnlook.features.sucursal.dto.RecomendacionesSucursalesDTO;
import com.gg.turnlook.features.sucursal.dto.SucursalMiniDTO;
import com.gg.turnlook.features.verificacion.canalEnvio.EmailService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:consulta-publica-recomendaciones;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
        "jwt.secret=local-test-secret-only-32-characters-minimum",
        "resend.api-key=test", "resend.from=test@example.com",
        "whatsapp.gateway.url=http://localhost:9999", "whatsapp.gateway.api-key=test",
        "mercadopago.access-token=test", "cloudinary.cloud-name=test",
        "cloudinary.api-key=test", "cloudinary.api-secret=test"
})
class ConsultaPublicaRecomendacionesTest {
    @LocalServerPort int port;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @MockitoBean SucursalService sucursales;
    @MockitoBean SucursalDescubrimientoService descubrimiento;
    @MockitoBean ReseniaService resenias;
    @MockitoBean EmailService email;

    @BeforeEach
    void prepararSeccionesPublicas() {
        SucursalMiniDTO publica = new SucursalMiniDTO(
                7, "Sucursal central", "PELUQUERIA", null, 4.5, 10L, List.of(), null, null);
        when(descubrimiento.obtenerRecomendaciones("anonymousUser")).thenReturn(
                new RecomendacionesSucursalesDTO(
                        List.of(publica), List.of(publica), List.of(publica),
                        List.of(), List.of(), List.of(publica)));
    }

    @Test
    void sinSesionDevuelveLasCuatroSeccionesPublicas() throws Exception {
        HttpResponse<String> respuesta = get("/sucursales/recomendaciones", null);

        assertEquals(200, respuesta.statusCode(), respuesta.body());
        JsonNode json = objectMapper.readTree(respuesta.body());
        assertEquals(1, json.path("mejores").size(), "body=" + respuesta.body());
        assertEquals(1, json.path("masDemandadas").size());
        assertEquals(1, json.path("nuevas").size());
        assertEquals(1, json.path("todas").size());
        assertEquals(0, json.path("vistasRecientes").size());
        assertEquals(0, json.path("paraVos").size());
        verify(descubrimiento).obtenerRecomendaciones("anonymousUser");
    }

    @Test
    void tokenInvalidoDegradaAVisitanteSoloEnLaLecturaPublica() throws Exception {
        HttpResponse<String> respuesta =
                get("/sucursales/recomendaciones", "Bearer token-invalido-test");

        assertEquals(200, respuesta.statusCode(), respuesta.body());
        JsonNode json = objectMapper.readTree(respuesta.body());
        assertEquals(1, json.path("mejores").size(), "body=" + respuesta.body());
        assertEquals(0, json.path("paraVos").size());
        verify(descubrimiento).obtenerRecomendaciones("anonymousUser");
    }

    @Test
    void tokenInvalidoSigueSiendo401EnUnEndpointProtegido() throws Exception {
        HttpResponse<String> respuesta =
                get("/sucursales/listar/propias", "Bearer token-invalido-test");

        assertEquals(401, respuesta.statusCode(), respuesta.body());
        verify(descubrimiento, never()).obtenerRecomendaciones(any());
    }

    private HttpResponse<String> get(String path, String authorization) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + path)).GET();
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
