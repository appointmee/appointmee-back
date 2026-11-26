package com.gg.turnlook.features.solicitud.empleador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gg.turnlook.common.exception.GlobalExceptionHandler;
import com.gg.turnlook.features.solicitud.empleador.dto.SolicitudEmpleadorCrearDTO;
import com.gg.turnlook.features.solicitud.empleador.dto.UbicacionSolicitudDTO;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/** Verifica el contrato HTTP del limite de ubicaciones al registrar. */
class SolicitudEmpleadorValidacionHttpTest {

    private static final String EMAIL = "ana@example.com";
    private static final String MENSAJE_LIMITE =
            "Una solicitud puede incluir como maximo 10 ubicaciones";

    private SolicitudEmpleadorService service;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        service = mock(SolicitudEmpleadorService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new SolicitudEmpleadorController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(EMAIL, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unaUbicacionEsAceptada() throws Exception {
        enviar(dto(1)).andExpect(status().isOk());

        assertEquals(1, capturarSolicitud().getUbicaciones().size());
    }

    @Test
    void diezUbicacionesSonAceptadas() throws Exception {
        enviar(dto(10)).andExpect(status().isOk());

        assertEquals(10, capturarSolicitud().getUbicaciones().size());
    }

    @Test
    void onceUbicacionesSonRechazadasCon400YMensajeDelLimite() throws Exception {
        enviar(dto(11))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ubicaciones").value(MENSAJE_LIMITE));

        verifyNoInteractions(service);
    }

    @Test
    void ceroUbicacionesSonRechazadasCon400PorLaValidacionExistente() throws Exception {
        enviar(dto(0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ubicaciones")
                        .value("Debe incluir al menos una ubicacion"));

        verifyNoInteractions(service);
    }

    private ResultActions enviar(SolicitudEmpleadorCrearDTO dto) throws Exception {
        return mockMvc.perform(post("/solicitudes-empleador/solicitar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)));
    }

    private SolicitudEmpleadorCrearDTO capturarSolicitud() {
        ArgumentCaptor<SolicitudEmpleadorCrearDTO> captor =
                ArgumentCaptor.forClass(SolicitudEmpleadorCrearDTO.class);
        verify(service).registrarSolicitud(captor.capture(), eq(EMAIL));
        return captor.getValue();
    }

    private SolicitudEmpleadorCrearDTO dto(int cantidad) {
        SolicitudEmpleadorCrearDTO dto = new SolicitudEmpleadorCrearDTO();
        dto.setMotivo("Quiero abrir una sucursal en el centro de la ciudad.");
        for (int i = 0; i < cantidad; i++) {
            UbicacionSolicitudDTO ubicacion = new UbicacionSolicitudDTO();
            ubicacion.setDireccion("Direccion " + (i + 1) + ", CABA");
            ubicacion.setLatitud(-34.0 - i / 1000.0);
            ubicacion.setLongitud(-58.0 - i / 1000.0);
            ubicacion.setGooglePlaceId("ChIJ_" + (i + 1));
            dto.getUbicaciones().add(ubicacion);
        }
        return dto;
    }
}
