package com.gg.turnlook.auth;

import com.gg.turnlook.features.servicio.ServicioService;
import com.gg.turnlook.features.sucursal.SucursalService;
import com.gg.turnlook.features.turno.TurnoService;
import com.gg.turnlook.features.turno.dto.DisponibilidadDTO;
import com.gg.turnlook.features.verificacion.canalEnvio.EmailService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:consulta-publica;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
        "jwt.secret=local-test-secret-only-32-characters-minimum",
        "resend.api-key=test", "resend.from=test@example.com",
        "whatsapp.gateway.url=http://localhost:9999", "whatsapp.gateway.api-key=test",
        "mercadopago.access-token=test", "cloudinary.cloud-name=test",
        "cloudinary.api-key=test", "cloudinary.api-secret=test"
})
class ConsultaPublicaReservaTest {
    @LocalServerPort int port;
    @MockitoBean ServicioService servicios;
    @MockitoBean SucursalService sucursales;
    @MockitoBean TurnoService turnos;
    @MockitoBean EmailService email;

    @Test
    void permiteConsultarServiciosProfesionalesYDisponibilidadSinSesion() throws Exception {
        when(servicios.listarServiciosSucursal(7)).thenReturn(List.of());
        when(sucursales.verEmpleadosParaElegir(7)).thenReturn(List.of());
        when(turnos.verDisponibilidad(9, 3)).thenReturn(List.of(
                new DisponibilidadDTO(LocalDate.of(2030, 12, 20), List.of(LocalTime.of(9, 0)))));

        for (String path : List.of("/servicios/listar/sucursal/7", "/sucursales/7/elegir-empleado",
                "/sucursales/7/resenias", "/turnos/disponibilidad/empleado/9/servicio/3")) {
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
            var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), path + ": " + response.body());
        }
        verify(turnos).verDisponibilidad(9, 3);
    }

    @Test
    void crearUnaReservaYConsultarTurnosPropiosSigueRequiriendoSesion() throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/turnos/registrar"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"fechaHora\":\"2030-12-20T09:00\",\"empleadoId\":9,\"servicioId\":3}"))
                .build();
        assertEquals(401, HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
        var propios = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/turnos/propios")).GET().build();
        assertEquals(401, HttpClient.newHttpClient().send(propios, HttpResponse.BodyHandlers.ofString()).statusCode());
        verify(turnos, never()).registrarTurno(any(), any());
    }
}
