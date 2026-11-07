package com.gg.turnlook.features.imagen;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gg.turnlook.features.imagen.dto.ImagenSubidaDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ImagenControllerTest {

    private ImagenCloudinaryService cloudinaryService;
    private ImagenPendienteService pendienteService;
    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        cloudinaryService = mock(ImagenCloudinaryService.class);
        pendienteService = mock(ImagenPendienteService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ImagenController(cloudinaryService, pendienteService)).build();
    }

    @Test
    void endpointRecibeMultipartYDevuelveUrlPublicIdYNombre() throws Exception {
        when(cloudinaryService.subir(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(ImagenCloudinaryTipo.PERFIL)))
                .thenReturn(new ImagenSubidaDTO(
                        "https://res.cloudinary.com/cloud/image/upload/appointmee/perfiles/foto.jpg",
                        "appointmee/perfiles/foto", "foto.jpg"));
        when(pendienteService.crearToken("appointmee/perfiles/foto", null)).thenReturn("token-temporal");

        mockMvc.perform(multipart("/imagenes/upload")
                        .file(new MockMultipartFile("file", "foto.jpg", "image/jpeg",
                                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}))
                        .param("tipo", "PERFIL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://res.cloudinary.com/cloud/image/upload/appointmee/perfiles/foto.jpg"))
                .andExpect(jsonPath("$.publicId").value("appointmee/perfiles/foto"))
                .andExpect(jsonPath("$.nombreArchivo").value("foto.jpg"))
                .andExpect(jsonPath("$.tokenCancelacion").value("token-temporal"));

        verify(cloudinaryService).subir(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(ImagenCloudinaryTipo.PERFIL));
    }

    @Test
    void endpointRechazaTipoNoPermitido() throws Exception {
        mockMvc.perform(multipart("/imagenes/upload")
                        .file(new MockMultipartFile("file", "foto.jpg", "image/jpeg",
                                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}))
                        .param("tipo", "ADMIN"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void endpointDescartaImagenPendiente() throws Exception {
        mockMvc.perform(delete("/imagenes/upload")
                        .contentType("application/json")
                        .content("{\"publicId\":\"appointmee/perfiles/foto\",\"tokenCancelacion\":\"token\"}"))
                .andExpect(status().isNoContent());
        verify(pendienteService).descartar("appointmee/perfiles/foto", "token", null);
    }
}
