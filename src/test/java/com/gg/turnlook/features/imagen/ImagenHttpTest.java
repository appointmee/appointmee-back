package com.gg.turnlook.features.imagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.gg.turnlook.auth.jwt.JwtService;
import com.gg.turnlook.features.usuario.Usuario;
import com.gg.turnlook.features.usuario.UsuarioRepository;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "cloudinary.cloud-name=cloud", "cloudinary.api-key=key", "cloudinary.api-secret=secret"
})
class ImagenHttpTest {

    @LocalServerPort int puerto;
    @Autowired JwtService jwtService;
    @Autowired UsuarioRepository usuarios;
    @MockitoBean Cloudinary cloudinary;
    private Uploader uploader;
    private Usuario usuario;
    private final HttpClient cliente = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void preparar() throws Exception {
        uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of(
                "secure_url", "https://res.cloudinary.com/cloud/image/upload/v123/appointmee/perfiles/foto.jpg",
                "public_id", "appointmee/perfiles/foto"));
        when(uploader.destroy(any(String.class), anyMap())).thenReturn(Map.of("result", "ok"));
        usuario = new Usuario("Ana", "Pérez", UUID.randomUUID() + "@example.com", "clave");
        usuario.setEmailVerificado(true);
        usuario = usuarios.saveAndFlush(usuario);
    }

    @AfterEach
    void limpiar() {
        usuarios.deleteById(usuario.getId());
    }

    @Test
    void exigeAutenticacionParaSubir() throws Exception {
        assertEquals(401, subir(new byte[]{(byte) 255, (byte) 216, (byte) 255}, null).statusCode());
        verify(uploader, never()).upload(any(), anyMap());
    }

    @Test
    void subeYDescartaConElMismoUsuarioPeroRechazaOtroUsuario() throws Exception {
        String token = jwtService.generarToken(usuario);
        HttpResponse<String> subida = subir(new byte[]{(byte) 255, (byte) 216, (byte) 255}, token);
        assertEquals(200, subida.statusCode(), subida.body());
        Map<?, ?> imagen = json.readValue(subida.body(), Map.class);
        String cuerpo = json.writeValueAsString(Map.of(
                "publicId", imagen.get("publicId"), "tokenCancelacion", imagen.get("tokenCancelacion")));

        Usuario inexistente = new Usuario("Otro", "Usuario", "inexistente@example.com", "clave");
        inexistente.setId(-1);
        assertEquals(401, descartar(cuerpo, jwtService.generarToken(inexistente)).statusCode());
        Usuario otro = new Usuario("Otro", "Usuario", UUID.randomUUID() + "@example.com", "clave");
        otro.setEmailVerificado(true);
        otro = usuarios.saveAndFlush(otro);
        try {
            assertEquals(400, descartar(cuerpo, jwtService.generarToken(otro)).statusCode());
        } finally {
            usuarios.deleteById(otro.getId());
        }
        verify(uploader, never()).destroy(any(String.class), anyMap());
        assertEquals(204, descartar(cuerpo, token).statusCode());
        verify(uploader).destroy(eq("appointmee/perfiles/foto"), anyMap());
    }

    @Test
    void rechazaMasDeCincoMbAntesDeLlamarACloudinary() throws Exception {
        byte[] contenido = new byte[5 * 1024 * 1024 + 1];
        contenido[0] = (byte) 255;
        contenido[1] = (byte) 216;
        contenido[2] = (byte) 255;
        assertEquals(413, subir(contenido, jwtService.generarToken(usuario)).statusCode());
        verify(uploader, never()).upload(any(), anyMap());
    }

    @Test
    void unTipoInvalidoEsErrorDelPedidoYNoErrorInterno() throws Exception {
        assertEquals(400, subir(new byte[]{(byte) 255, (byte) 216, (byte) 255},
                jwtService.generarToken(usuario), "ADMIN").statusCode());
        verify(uploader, never()).upload(any(), anyMap());
    }

    private HttpResponse<String> subir(byte[] contenido, String token) throws Exception {
        return subir(contenido, token, "PERFIL");
    }

    private HttpResponse<String> subir(byte[] contenido, String token, String tipo) throws Exception {
        String limite = "appointmee-boundary";
        ByteArrayOutputStream cuerpo = new ByteArrayOutputStream();
        cuerpo.write(("--" + limite + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"foto.jpg\""
                + "\r\nContent-Type: image/jpeg\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        cuerpo.write(contenido);
        cuerpo.write(("\r\n--" + limite + "--\r\n").getBytes(StandardCharsets.UTF_8));
        HttpRequest.Builder solicitud = HttpRequest.newBuilder(URI.create(
                "http://localhost:" + puerto + "/imagenes/upload?tipo=" + tipo))
                .header("Content-Type", "multipart/form-data; boundary=" + limite)
                .POST(HttpRequest.BodyPublishers.ofByteArray(cuerpo.toByteArray()));
        if (token != null) solicitud.header("Authorization", "Bearer " + token);
        return cliente.send(solicitud.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> descartar(String cuerpo, String token) throws Exception {
        return cliente.send(HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/imagenes/upload"))
                .header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
                .method("DELETE", HttpRequest.BodyPublishers.ofString(cuerpo)).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
