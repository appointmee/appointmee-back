package com.gg.turnlook.auth;

import com.gg.turnlook.auth.dto.*;
import com.gg.turnlook.auth.jwt.JwtService;
import com.gg.turnlook.auth.refresh.*;
import com.gg.turnlook.auth.password.*;
import com.gg.turnlook.features.usuario.*;
import com.gg.turnlook.features.usuario.rol.*;
import com.gg.turnlook.features.verificacion.canalEnvio.EmailService;
import com.gg.turnlook.common.exception.*;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:auth-integration;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
    "spring.datasource.password=", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
    "jwt.secret=local-test-secret-only-32-characters-minimum",
    "resend.api-key=test", "resend.from=test@example.com",
    "whatsapp.gateway.url=http://localhost:9999", "whatsapp.gateway.api-key=test",
    "mercadopago.access-token=test", "cloudinary.cloud-name=test",
    "cloudinary.api-key=test", "cloudinary.api-secret=test"
})
class AuthIntegrationTest {
    @org.springframework.boot.test.web.server.LocalServerPort int port;
    @Autowired AuthService auth;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean RefreshTokenService emisorRefresh;
    @Autowired UsuarioService usuarioService;
    @Autowired CambioCorreoService cambioCorreo;
    @Autowired com.gg.turnlook.features.verificacion.VerificacionEmailService verificacionEmail;
    @Autowired com.gg.turnlook.features.verificacion.VerificacionRepository codigos;
    @Autowired PasswordResetService passwords;
    @Autowired RefreshTokenRepository refreshTokens;
    @Autowired PasswordResetTokenRepository resetTokens;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean EmailService email;
    private Usuario usuario;
    private static final String PASSWORD = "Password123!";
    @BeforeEach void preparar() {
        usuario = new Usuario("Ana", "Perez", UUID.randomUUID() + "@example.com", encoder.encode(PASSWORD));
        usuario.setEmailVerificado(true);
        usuario = usuarios.saveAndFlush(usuario);
    }
    @AfterEach void limpiar() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            refreshTokens.revocarPorUsuario(usuario.getId());
            resetTokens.revocarPorUsuario(usuario.getId());
            em.createQuery("delete from Verificacion v where v.usuario.id = :id").setParameter("id", usuario.getId()).executeUpdate();
            usuarios.deleteById(usuario.getId());
        });
    }
    private LoginResponseDTO login() { return auth.login(new LoginDTO(usuario.getEmail(), PASSWORD)); }
    @Test void rotaYRechazaReutilizacionYNoGuardaElTokenOriginal() {
        var primero = login();
        assertNotEquals(primero.getToken(), primero.getRefreshToken());
        assertEquals(TokenSeguro.hash(primero.getRefreshToken()), refreshTokens.findByUsuarioId(usuario.getId()).orElseThrow().getTokenHash());
        var segundo = auth.renovar(primero.getRefreshToken());
        assertNotEquals(primero.getRefreshToken(), segundo.getRefreshToken());
        assertThrows(UnauthorizedException.class, () -> auth.renovar(primero.getRefreshToken()));
        assertThrows(UnauthorizedException.class, () -> auth.renovar(segundo.getToken()));
        assertThrows(io.jsonwebtoken.JwtException.class, () -> jwt.validarAccessToken(segundo.getRefreshToken()));
    }
    @Test void unaSolaRenovacionConcurrenteTieneExito() throws Exception {
        var sesion = login();
        var inicio = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> tarea = () -> { inicio.await(); try { auth.renovar(sesion.getRefreshToken()); return true; } catch (UnauthorizedException ex) { return false; } };
            var a = pool.submit(tarea); var b = pool.submit(tarea); inicio.countDown();
            assertNotEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
        }
    }
    @Test void logoutYSegundoLoginRevocanRefresh() {
        var anterior = login(); var actual = login();
        assertThrows(UnauthorizedException.class, () -> auth.renovar(anterior.getRefreshToken()));
        auth.logout(usuario.getEmail());
        assertThrows(UnauthorizedException.class, () -> auth.renovar(actual.getRefreshToken()));
    }
    @Test void rechazaRefreshVencidoYCuentaInactiva() {
        var sesion = login();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var token = refreshTokens.findByUsuarioId(usuario.getId()).orElseThrow();
            token.setFechaVencimiento(Instant.now().minusSeconds(1));
        });
        assertThrows(UnauthorizedException.class, () -> auth.renovar(sesion.getRefreshToken()));
        var nueva = login();
        usuario.setActivo(false); usuarios.saveAndFlush(usuario);
        assertThrows(UnauthorizedException.class, () -> auth.renovar(nueva.getRefreshToken()));
    }
    @Test void recuperacionDeUnSoloUsoRevocaSesionesYActualizaHash() {
        var sesion = login();
        var enlace = new AtomicReference<String>();
        doAnswer(invocation -> { enlace.set(invocation.getArgument(1)); return null; }).when(email).enviarRecuperacion(eq(usuario.getEmail()), anyString(), anyLong());
        passwords.solicitar(usuario.getEmail());
        String token = enlace.get().split("token=")[1];
        assertTrue(resetTokens.findByTokenHash(TokenSeguro.hash(token)).isPresent());
        assertTrue(passwords.validarEnlace(token).valido());
        assertTrue(passwords.validarEnlace(token).expiresInSeconds() > 0);
        assertTrue(passwords.validarEnlace(token).valido()); // Consultarlo no lo consume.
        passwords.restablecer(token, "NuevaPassword123!");
        verify(email).enviarAvisoCambioPassword(usuario.getEmail());
        assertFalse(passwords.validarEnlace(token).valido());
        assertTrue(encoder.matches("NuevaPassword123!", usuarios.findById(usuario.getId()).orElseThrow().getPassword()));
        assertEquals(1, usuarios.findById(usuario.getId()).orElseThrow().getCredencialesVersion());
        assertThrows(BadRequestException.class, () -> passwords.restablecer(token, PASSWORD));
        assertThrows(UnauthorizedException.class, () -> auth.renovar(sesion.getRefreshToken()));
    }
    @Test void cambioExigePasswordActualYRevocaRefresh() {
        var sesion = login();
        assertThrows(BadRequestException.class, () -> auth.cambiarPassword(usuario.getEmail(), new ChangePasswordRequest("incorrecta", "NuevaPassword123!")));
        auth.cambiarPassword(usuario.getEmail(), new ChangePasswordRequest(PASSWORD, "NuevaPassword123!"));
        assertThrows(UnauthorizedException.class, () -> auth.renovar(sesion.getRefreshToken()));
        verify(email).enviarAvisoCambioPassword(usuario.getEmail());
    }

    @Test void passwordIgualOActualIncorrectaNoModificanNiEnvianAviso() {
        assertThrows(BadRequestException.class, () -> auth.cambiarPassword(usuario.getEmail(), new ChangePasswordRequest(PASSWORD, PASSWORD)));
        assertThrows(BadRequestException.class, () -> auth.cambiarPassword(usuario.getEmail(), new ChangePasswordRequest("incorrecta", "NuevaPassword123!")));
        assertTrue(encoder.matches(PASSWORD, usuarios.findById(usuario.getId()).orElseThrow().getPassword()));
        verify(email, never()).enviarAvisoCambioPassword(anyString());
    }

    @Test void endpointCambioValidaPasswordSinCerrarLaSesionPorUnDatoIncorrecto() throws Exception {
        var sesion = login();
        var client = java.net.http.HttpClient.newHttpClient();
        var request = java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost:" + port + "/auth/change-password"))
                .header("Authorization", "Bearer " + sesion.getToken()).header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{\"currentPassword\":\"incorrecta\",\"newPassword\":\"NuevaPassword123!\"}")).build();
        var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("CURRENT_PASSWORD_INVALID"));
        verify(email, never()).enviarAvisoCambioPassword(anyString());
        assertDoesNotThrow(() -> auth.renovar(sesion.getRefreshToken()));
        var debil = java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost:" + port + "/auth/change-password"))
                .header("Authorization", "Bearer " + sesion.getToken()).header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"simple\"}")).build();
        assertEquals(400, client.send(debil, java.net.http.HttpResponse.BodyHandlers.ofString()).statusCode());
        assertTrue(encoder.matches(PASSWORD, usuarios.findById(usuario.getId()).orElseThrow().getPassword()));
    }

    @Test void avisoSeEnviaSoloDespuesDelCommitYNoEnRollback() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            auth.cambiarPassword(usuario.getEmail(), new ChangePasswordRequest(PASSWORD, "NuevaPassword123!"));
            verify(email, never()).enviarAvisoCambioPassword(anyString());
            status.setRollbackOnly();
        });
        verify(email, never()).enviarAvisoCambioPassword(anyString());
        assertTrue(encoder.matches(PASSWORD, usuarios.findById(usuario.getId()).orElseThrow().getPassword()));
    }

    @Test void falloDelCorreoNoRevierteCambioNiReactivaLaSesion() {
        var sesion = login();
        doThrow(new org.springframework.web.client.RestClientException("Proveedor no disponible"))
                .when(email).enviarAvisoCambioPassword(anyString());
        assertDoesNotThrow(() -> auth.cambiarPassword(usuario.getEmail(), new ChangePasswordRequest(PASSWORD, "NuevaPassword123!")));
        assertTrue(encoder.matches("NuevaPassword123!", usuarios.findById(usuario.getId()).orElseThrow().getPassword()));
        assertThrows(UnauthorizedException.class, () -> auth.renovar(sesion.getRefreshToken()));
    }
    @Test void correoInexistenteNoEnviaEmail() {
        passwords.solicitar("nadie@example.com");
        verifyNoInteractions(email);
    }

    private int get(String token) throws Exception {
        var request = java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost:" + port + "/usuarios/listar"))
                .header("Authorization", "Bearer " + token).GET().build();
        return java.net.http.HttpClient.newHttpClient().send(request, java.net.http.HttpResponse.BodyHandlers.ofString()).statusCode();
    }
    @Test void filtroUsaRolesActualesYRechazaTokenRevocado() throws Exception {
        var sesion = login();
        assertEquals(403, get(sesion.getToken()));
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Rol admin = new Rol(); admin.setRol(ERol.ADMINISTRADOR); em.persist(admin);
            em.find(Usuario.class, usuario.getId()).getRoles().add(admin);
        });
        assertEquals(200, get(sesion.getToken()));
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> em.find(Usuario.class, usuario.getId()).getRoles().clear());
        assertEquals(403, get(sesion.getToken()));
        auth.cambiarPassword(usuario.getEmail(), new ChangePasswordRequest(PASSWORD, "NuevaPassword123!"));
        assertEquals(401, get(sesion.getToken()));
        assertEquals(401, get(sesion.getRefreshToken()));
    }
    @Test void endpointsPublicosValidanDatosYRefreshNoDependeDelAccess() throws Exception {
        var sesion = login();
        var client = java.net.http.HttpClient.newHttpClient();
        var request = java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost:" + port + "/auth/refresh"))
                .header("Content-Type", "application/json").header("Authorization", "Bearer invalido")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{\"refreshToken\":\"" + sesion.getRefreshToken() + "\"}")).build();
        assertEquals(200, client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString()).statusCode());
        var invalid = java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost:" + port + "/auth/reset-password"))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{\"token\":\"x\",\"newPassword\":\"123\"}")).build();
        assertEquals(400, client.send(invalid, java.net.http.HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    @Test void recuperacionVencidaNoCambiaPasswordYLimitaReenvios() {
        var enlace = new AtomicReference<String>();
        doAnswer(invocation -> { enlace.set(invocation.getArgument(1)); return null; }).when(email).enviarRecuperacion(eq(usuario.getEmail()), anyString(), anyLong());
        passwords.solicitar(usuario.getEmail());
        passwords.solicitar(usuario.getEmail());
        verify(email, times(1)).enviarRecuperacion(eq(usuario.getEmail()), anyString(), anyLong());
        String token = enlace.get().split("token=")[1];
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            resetTokens.findByTokenHash(TokenSeguro.hash(token)).orElseThrow().setFechaVencimiento(Instant.now().minusSeconds(1));
        });
        assertThrows(BadRequestException.class, () -> passwords.restablecer(token, "OtraPassword123!"));
        assertFalse(passwords.validarEnlace(token).valido());
        assertTrue(encoder.matches(PASSWORD, usuarios.findById(usuario.getId()).orElseThrow().getPassword()));
    }

    @Test void validacionPublicaDelEnlaceNoRequiereSesionNiDevuelveElToken() throws Exception {
        String token = TokenSeguro.generar();
        PasswordResetToken registro = new PasswordResetToken();
        registro.setUsuario(usuario);
        registro.setTokenHash(TokenSeguro.hash(token));
        registro.setFechaVencimiento(Instant.now().plusSeconds(600));
        resetTokens.saveAndFlush(registro);

        var response = post("/auth/reset-password/validate", "{\"token\":\"" + token + "\"}");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"valido\":true"));
        assertFalse(response.body().contains(token));
        assertTrue(response.headers().firstValue("Cache-Control").orElse("").contains("no-store"));
        assertTrue(resetTokens.findByTokenHash(TokenSeguro.hash(token)).isPresent());

        registro.setFechaUso(Instant.now());
        resetTokens.saveAndFlush(registro);
        assertFalse(passwords.validarEnlace(token).valido());
        assertFalse(passwords.validarEnlace(TokenSeguro.generar()).valido());
    }

    private void pendiente() {
        usuario.setEmailVerificado(false);
        usuarios.saveAndFlush(usuario);
    }

    private String solicitarCodigo() {
        var codigo = new AtomicReference<String>();
        doAnswer(invocation -> { codigo.set(invocation.getArgument(1)); return null; })
                .when(email).enviarCodigo(eq(usuario.getEmail()), anyString());
        verificacionEmail.solicitar(usuario.getEmail());
        return codigo.get();
    }

    private String ticket() {
        return jwt.generarTokenVerificacion(usuarios.findById(usuario.getId()).orElseThrow());
    }

    private java.net.http.HttpResponse<String> post(String path, String body) throws Exception {
        return postConAcceso(path, body, path.contains("verification") || path.equals("/auth/verify-email") ? ticket() : null);
    }

    private java.net.http.HttpResponse<String> postConAcceso(String path, String body, String acceso) throws Exception {
        var builder = java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer access-vencido");
        if (acceso != null) builder.header("X-Verification-Token", acceso);
        var request = builder.POST(java.net.http.HttpRequest.BodyPublishers.ofString(body)).build();
        return java.net.http.HttpClient.newHttpClient().send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
    }

    @Test void loginPendienteSoloRevelaEstadoTrasPasswordCorrecta() throws Exception {
        pendiente();
        var incorrecto = post("/auth/login", "{\"email\":\"" + usuario.getEmail() + "\",\"pass\":\"incorrecta\"}");
        assertEquals(401, incorrecto.statusCode());
        assertFalse(incorrecto.body().contains("EMAIL_NOT_VERIFIED"));
        var correcto = post("/usuarios/inicio-sesion", "{\"email\":\"" + usuario.getEmail() + "\",\"pass\":\"" + PASSWORD + "\"}");
        assertEquals(403, correcto.statusCode());
        assertTrue(correcto.body().contains("EMAIL_NOT_VERIFIED"));
        assertFalse(correcto.body().contains("refreshToken"));
        assertTrue(refreshTokens.findByUsuarioId(usuario.getId()).isEmpty());
    }

    @Test void verificaEmailPorHttpYPermiteLoginSinSesionPrevia() throws Exception {
        pendiente();
        var codigo = new AtomicReference<String>();
        doAnswer(invocation -> { codigo.set(invocation.getArgument(1)); return null; }).when(email).enviarCodigo(eq(usuario.getEmail()), anyString());
        assertEquals(202, post("/auth/send-verification", "{\"email\":\"" + usuario.getEmail().toUpperCase() + "\"}").statusCode());
        assertTrue(codigo.get().matches("[0-9]{6}"));
        var registro = codigos.findFirstByUsuarioIdAndCanalOrderByFechaCreacionDescIdDesc(usuario.getId(), com.gg.turnlook.features.verificacion.enums.CanalVerificacion.EMAIL).orElseThrow();
        assertNotEquals(codigo.get(), registro.getCodigoHash());
        assertTrue(encoder.matches(codigo.get(), registro.getCodigoHash()));
        var confirmado = post("/auth/verify-email", "{\"email\":\"" + usuario.getEmail() + "\",\"codigo\":\"" + codigo.get() + "\"}");
        assertEquals(200, confirmado.statusCode(), confirmado.body());
        assertTrue(usuarios.findById(usuario.getId()).orElseThrow().isEmailVerificado());
        assertNotNull(login().getRefreshToken());
        assertThrows(BadRequestException.class, () -> verificacionEmail.confirmar(usuario.getEmail(), codigo.get()));
    }

    @Test void cincoIntentosFallidosPersistenYBloqueanElCodigoCorrecto() {
        pendiente(); String codigo = solicitarCodigo();
        String incorrecto = codigo.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 5; i++) {
            assertThrows(BadRequestException.class, () -> verificacionEmail.confirmar(usuario.getEmail(), incorrecto));
        }
        assertEquals(5, codigos.findFirstByUsuarioIdAndCanalOrderByFechaCreacionDescIdDesc(usuario.getId(), com.gg.turnlook.features.verificacion.enums.CanalVerificacion.EMAIL).orElseThrow().getIntentosFallidos());
        assertThrows(BadRequestException.class, () -> verificacionEmail.confirmar(usuario.getEmail(), codigo));
        assertFalse(usuarios.findById(usuario.getId()).orElseThrow().isEmailVerificado());
    }

    @Test void codigoVencidoNoVerificaYNoPermiteReenviosInmediatos() {
        pendiente(); String codigo = solicitarCodigo();
        verificacionEmail.solicitar(usuario.getEmail());
        verify(email, times(1)).enviarCodigo(eq(usuario.getEmail()), anyString());
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            codigos.findFirstByUsuarioIdAndCanalOrderByFechaCreacionDescIdDesc(usuario.getId(), com.gg.turnlook.features.verificacion.enums.CanalVerificacion.EMAIL).orElseThrow().setFechaVencimiento(Instant.now().minusSeconds(1));
        });
        assertThrows(BadRequestException.class, () -> verificacionEmail.confirmar(usuario.getEmail(), codigo));
    }

    @Test void reenvioInvalidaElCodigoAnterior() {
        pendiente(); String anterior = solicitarCodigo();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            codigos.findFirstByUsuarioIdAndCanalOrderByFechaCreacionDescIdDesc(usuario.getId(), com.gg.turnlook.features.verificacion.enums.CanalVerificacion.EMAIL).orElseThrow().setFechaCreacion(Instant.now().minusSeconds(61));
        });
        String nuevo = solicitarCodigo();
        var registros = codigos.findAll().stream().filter(v -> v.getUsuario() != null && v.getUsuario().getId().equals(usuario.getId())).toList();
        assertEquals(2, registros.size());
        assertEquals(1, registros.stream().filter(v -> v.getFechaUso() == null).count());
        assertNotEquals(anterior, nuevo);
        assertThrows(BadRequestException.class, () -> verificacionEmail.confirmar(usuario.getEmail(), anterior));
        verificacionEmail.confirmar(usuario.getEmail(), nuevo);
        assertTrue(usuarios.findById(usuario.getId()).orElseThrow().isEmailVerificado());
    }

    @Test void envioNoRevelaCorreoInexistenteOVerificado() throws Exception {
        var respuesta = post("/auth/send-verification", "{\"email\":\"no-existe@example.com\"}");
        var verificado = post("/auth/send-verification", "{\"email\":\"" + usuario.getEmail() + "\"}");
        assertEquals(401, respuesta.statusCode());
        assertEquals(202, verificado.statusCode());
        verify(email, never()).enviarCodigo(anyString(), anyString());
    }

    @Test void emailPendienteNoPuedeUsarJwtNiRefreshPrevios() throws Exception {
        var sesion = login();
        pendiente();
        assertEquals(401, get(sesion.getToken()));
        assertThrows(UnauthorizedException.class, () -> auth.renovar(sesion.getRefreshToken()));
    }

    @Test void codigoGenericoNoSirveComoCodigoDeCuenta() {
        pendiente();
        codigos.saveAndFlush(com.gg.turnlook.features.verificacion.Verificacion.builder()
                .canal(com.gg.turnlook.features.verificacion.enums.CanalVerificacion.EMAIL)
                .destinatario(usuario.getEmail()).codigoHash(encoder.encode("123456"))
                .fechaCreacion(Instant.now()).fechaVencimiento(Instant.now().plusSeconds(300))
                .intentosFallidos(0).build());
        try {
            assertThrows(BadRequestException.class, () -> verificacionEmail.confirmar(usuario.getEmail(), "123456"));
        } finally {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> em.createQuery("delete from Verificacion v where v.usuario is null and v.destinatario = :email").setParameter("email", usuario.getEmail()).executeUpdate());
        }
    }

    @Test void registroCreaCuentaPendienteYEnviaCodigo() throws Exception {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var rol = com.gg.turnlook.features.usuario.rol.ERol.CLIENTE;
            if (em.createQuery("select r from Rol r where r.rol = :rol", com.gg.turnlook.features.usuario.rol.Rol.class).setParameter("rol", rol).getResultList().isEmpty()) {
                var nuevoRol = new com.gg.turnlook.features.usuario.rol.Rol(); nuevoRol.setRol(rol); em.persist(nuevoRol);
            }
        });
        String emailRegistro = UUID.randomUUID() + "@example.com";
        var respuesta = post("/auth/register", "{\"nombre\":\"Ana\",\"apellido\":\"Perez\",\"email\":\"" + emailRegistro + "\",\"password\":\"" + PASSWORD + "\"}");
        assertEquals(201, respuesta.statusCode(), respuesta.body());
        var registrado = usuarios.findByEmailAndActivoTrue(emailRegistro).orElseThrow();
        try {
            assertFalse(registrado.isEmailVerificado());
            assertTrue(registrado.getRoles().stream().allMatch(r -> r.getRol() == com.gg.turnlook.features.usuario.rol.ERol.CLIENTE));
            verify(email).enviarCodigo(eq(emailRegistro), matches("[0-9]{6}"));
            assertThrows(EmailNoVerificadoException.class, () -> auth.login(new LoginDTO(emailRegistro, PASSWORD)));
        } finally {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                var usuarioNuevo = em.find(Usuario.class, registrado.getId());
                var foto = usuarioNuevo.getFotoPerfil();
                em.createQuery("delete from Verificacion v where v.usuario.id = :id").setParameter("id", usuarioNuevo.getId()).executeUpdate();
                em.remove(usuarioNuevo); em.flush(); if (foto != null) em.remove(foto);
            });
        }
    }

    @Test void cambioDeCorreoConfirmaElNuevoYNotificaElAnterior() throws Exception {
        var sesion = login();
        String anterior = usuario.getEmail();
        String nuevo = UUID.randomUUID() + "@example.com";
        var codigo = org.mockito.ArgumentCaptor.forClass(String.class);
        String datos = "{\"email\":\"" + nuevo + "\"}";
        assertEquals(202, postAutenticado("/usuarios/cambio-email/solicitar", datos, sesion.getToken()).statusCode());
        verify(email).enviarCodigoCambioCorreo(eq(nuevo), codigo.capture());
        assertEquals(anterior, usuarios.findById(usuario.getId()).orElseThrow().getEmail());
        verify(email, never()).enviarAvisoCambioCorreo(anyString());

        String confirmacion = "{\"email\":\"" + nuevo + "\",\"codigo\":\"" + codigo.getValue() + "\"}";
        assertEquals(204, postAutenticado("/usuarios/cambio-email/confirmar", confirmacion, sesion.getToken()).statusCode());
        var actualizado = usuarios.findById(usuario.getId()).orElseThrow();
        assertEquals(nuevo, actualizado.getEmail());
        assertTrue(actualizado.isEmailVerificado());
        verify(email).enviarAvisoCambioCorreo(anterior);
        assertThrows(UnauthorizedException.class, () -> auth.renovar(sesion.getRefreshToken()));
        assertNotNull(auth.login(new LoginDTO(nuevo, PASSWORD)).getToken());
    }

    private java.net.http.HttpResponse<String> postAutenticado(String path, String body, String token) throws Exception {
        var request = java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body)).build();
        return java.net.http.HttpClient.newHttpClient().send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
    }

    @Test void verificarEmailIniciaSesionYElCodigoNoSePuedeReutilizar() throws Exception {
        pendiente(); String codigo = solicitarCodigo();
        var respuesta = post("/auth/verify-email", "{\"email\":\"" + usuario.getEmail() + "\",\"codigo\":\"" + codigo + "\"}");
        assertEquals(200, respuesta.statusCode(), respuesta.body());
        var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(respuesta.body());
        assertEquals(usuario.getId(), json.get("usuario").get("id").asInt());
        String access = json.get("accessToken").asText();
        assertEquals(usuario.getId(), jwt.validarAccessToken(access).get("id", Integer.class));
        assertNotNull(auth.renovar(json.get("refreshToken").asText()));
        assertEquals(409, post("/auth/verify-email", "{\"email\":\"" + usuario.getEmail() + "\",\"codigo\":\"" + codigo + "\"}").statusCode());
    }

    @Test void primerReenvioEsA15SegundosYElSiguienteA60() {
        pendiente(); solicitarCodigo();
        verificacionEmail.solicitar(usuario.getEmail());
        verify(email, times(1)).enviarCodigo(eq(usuario.getEmail()), anyString());
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> codigos
                .findFirstByUsuarioIdAndCanalOrderByFechaCreacionDescIdDesc(usuario.getId(), com.gg.turnlook.features.verificacion.enums.CanalVerificacion.EMAIL)
                .orElseThrow().setFechaCreacion(Instant.now().minusSeconds(16)));
        verificacionEmail.solicitar(usuario.getEmail());
        verify(email, times(2)).enviarCodigo(eq(usuario.getEmail()), anyString());
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> codigos
                .findFirstByUsuarioIdAndCanalOrderByFechaCreacionDescIdDesc(usuario.getId(), com.gg.turnlook.features.verificacion.enums.CanalVerificacion.EMAIL)
                .orElseThrow().setFechaCreacion(Instant.now().minusSeconds(16)));
        verificacionEmail.solicitar(usuario.getEmail());
        verify(email, times(2)).enviarCodigo(eq(usuario.getEmail()), anyString());
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> em.createQuery(
                "update Verificacion v set v.fechaCreacion = :fecha where v.usuario.id = :id")
                .setParameter("fecha", Instant.now().minusSeconds(61)).setParameter("id", usuario.getId()).executeUpdate());
        verificacionEmail.solicitar(usuario.getEmail());
        verify(email, times(3)).enviarCodigo(eq(usuario.getEmail()), anyString());
    }

    @Test void intentosFallidosDesdeElControllerTambienPersisten() throws Exception {
        pendiente(); String codigo = solicitarCodigo();
        String incorrecto = codigo.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 5; i++) {
            assertEquals(400, post("/auth/verify-email", "{\"email\":\"" + usuario.getEmail() + "\",\"codigo\":\"" + incorrecto + "\"}").statusCode());
        }
        assertEquals(5, codigos.findFirstByUsuarioIdAndCanalOrderByFechaCreacionDescIdDesc(usuario.getId(), com.gg.turnlook.features.verificacion.enums.CanalVerificacion.EMAIL).orElseThrow().getIntentosFallidos());
        assertThrows(BadRequestException.class, () -> auth.verificarEmail(usuario.getEmail(), codigo, ticket()));
        assertTrue(refreshTokens.findByUsuarioId(usuario.getId()).isEmpty());
    }

    @Test void verificacionExigeCredencialesPreviasYElTicketNoSirveParaUsarLaApi() throws Exception {
        pendiente();
        String cuerpo = "{\"email\":\"" + usuario.getEmail() + "\"}";
        assertEquals(401, postConAcceso("/auth/verification-status", cuerpo, null).statusCode());
        assertEquals(401, postConAcceso("/auth/send-verification", cuerpo, "falso").statusCode());
        assertEquals(401, postConAcceso("/auth/send-verification", cuerpo, jwt.generarToken(usuario)).statusCode());
        var login = postConAcceso("/auth/login", "{\"email\":\"" + usuario.getEmail() + "\",\"pass\":\"" + PASSWORD + "\"}", null);
        assertEquals(403, login.statusCode());
        String acceso = new com.fasterxml.jackson.databind.ObjectMapper().readTree(login.body()).get("verificationToken").asText();
        assertEquals(200, postConAcceso("/auth/verification-status", cuerpo, acceso).statusCode());
        assertEquals(401, get(acceso));
        assertEquals(401, postConAcceso("/auth/send-verification", "{\"email\":\"otro@example.com\"}", acceso).statusCode());
        assertEquals(400, postConAcceso("/auth/complete-verification", cuerpo, acceso).statusCode());
    }

    @Test void recargarOVolverAlFlujoNoInvalidaElCodigoYElRelojVieneDelServidor() throws Exception {
        pendiente(); String codigo = solicitarCodigo();
        var status = new com.fasterxml.jackson.databind.ObjectMapper().readTree(post("/auth/verification-status", "{\"email\":\"" + usuario.getEmail() + "\"}").body());
        assertTrue(status.get("enviado").asBoolean());
        assertTrue(status.get("retryAfterSeconds").asLong() > 0);
        assertTrue(status.get("retryAfterSeconds").asLong() <= 15);
        var inicio = post("/auth/send-verification", "{\"email\":\"" + usuario.getEmail() + "\",\"reenviar\":false}");
        assertEquals(202, inicio.statusCode());
        assertFalse(new com.fasterxml.jackson.databind.ObjectMapper().readTree(inicio.body()).get("enviadoAhora").asBoolean());
        verify(email, times(1)).enviarCodigo(eq(usuario.getEmail()), anyString());
        assertEquals(200, post("/auth/verify-email", "{\"email\":\"" + usuario.getEmail() + "\",\"codigo\":\"" + codigo + "\"}").statusCode());
    }

    @Test void unReenvioAntesDeTiempoNoDiceQueEnvioYConservaElCodigoValido() throws Exception {
        pendiente(); String codigo = solicitarCodigo();
        var envio = post("/auth/send-verification", "{\"email\":\"" + usuario.getEmail() + "\",\"reenviar\":true}");
        var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(envio.body());
        assertEquals(202, envio.statusCode()); assertFalse(json.get("enviadoAhora").asBoolean());
        assertTrue(json.get("retryAfterSeconds").asLong() > 0);
        verify(email, times(1)).enviarCodigo(eq(usuario.getEmail()), anyString());
        verificacionEmail.confirmar(usuario.getEmail(), codigo);
    }

    @Test void falloDelProveedorNoInvalidaCodigoAnteriorNiSeOcultaConUn202() throws Exception {
        pendiente(); String codigo = solicitarCodigo();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> codigos
                .findFirstByUsuarioIdAndCanalOrderByFechaCreacionDescIdDesc(usuario.getId(), com.gg.turnlook.features.verificacion.enums.CanalVerificacion.EMAIL)
                .orElseThrow().setFechaCreacion(Instant.now().minusSeconds(61)));
        doThrow(new org.springframework.web.client.RestClientException("Proveedor no disponible")).when(email).enviarCodigo(eq(usuario.getEmail()), anyString());
        assertEquals(503, post("/auth/send-verification", "{\"email\":\"" + usuario.getEmail() + "\",\"reenviar\":true}").statusCode());
        verificacionEmail.confirmar(usuario.getEmail(), codigo);
        assertTrue(usuarios.findById(usuario.getId()).orElseThrow().isEmailVerificado());
    }

    @Test void respuestaPerdidaSeRecuperaConElMismoTicketSinReutilizarElCodigo() throws Exception {
        pendiente(); String codigo = solicitarCodigo(); String acceso = ticket();
        String cuerpo = "{\"email\":\"" + usuario.getEmail() + "\"}";
        assertEquals(200, postConAcceso("/auth/verify-email", "{\"email\":\"" + usuario.getEmail() + "\",\"codigo\":\"" + codigo + "\"}", acceso).statusCode());
        var estado = postConAcceso("/auth/verification-status", cuerpo, acceso);
        assertTrue(new com.fasterxml.jackson.databind.ObjectMapper().readTree(estado.body()).get("verificada").asBoolean());
        var completo = postConAcceso("/auth/complete-verification", cuerpo, acceso);
        assertEquals(200, completo.statusCode());
        assertTrue(completo.body().contains("refreshToken"));
        assertEquals(409, postConAcceso("/auth/verify-email", "{\"email\":\"" + usuario.getEmail() + "\",\"codigo\":\"" + codigo + "\"}", acceso).statusCode());
    }

    @Test void contadorDeIntentosYLímiteSeInformanConElEstadoReal() throws Exception {
        pendiente(); String codigo = solicitarCodigo();
        String mal = codigo.equals("000000") ? "111111" : "000000";
        for (int intento = 1; intento <= 5; intento++) {
            var respuesta = post("/auth/verify-email", "{\"email\":\"" + usuario.getEmail() + "\",\"codigo\":\"" + mal + "\"}");
            var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(respuesta.body());
            assertEquals(400, respuesta.statusCode()); assertEquals(5 - intento, json.get("attemptsRemaining").asInt());
            if (intento == 5) assertEquals("VERIFICATION_ATTEMPTS_EXCEEDED", json.get("code").asText());
        }
        assertFalse(usuarios.findById(usuario.getId()).orElseThrow().isEmailVerificado());
    }

    @Test void falloAlCrearSesionNoDejaEmailVerificadoNiCodigoConsumido() {
        pendiente(); String codigo = solicitarCodigo(); String acceso = ticket();
        doThrow(new BadRequestException("No se pudo crear la sesión")).when(emisorRefresh).emitir(any(Usuario.class));
        assertThrows(BadRequestException.class, () -> auth.verificarEmail(usuario.getEmail(), codigo, acceso));
        assertFalse(usuarios.findById(usuario.getId()).orElseThrow().isEmailVerificado());
        assertNull(codigos.findFirstByUsuarioIdAndCanalOrderByFechaCreacionDescIdDesc(usuario.getId(), com.gg.turnlook.features.verificacion.enums.CanalVerificacion.EMAIL).orElseThrow().getFechaUso());
        assertTrue(refreshTokens.findByUsuarioId(usuario.getId()).isEmpty());
    }

    @Test void ticketVencidoOCredencialesCambiadasNoPermitenVerificar() throws Exception {
        pendiente(); String cuerpo = "{\"email\":\"" + usuario.getEmail() + "\"}";
        String vencido = io.jsonwebtoken.Jwts.builder().subject(usuario.getEmail()).claim("id", usuario.getId())
                .claim("version", usuario.getCredencialesVersion()).claim("type", "email-verification")
                .expiration(new java.util.Date(System.currentTimeMillis() - 1000))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor("local-test-secret-only-32-characters-minimum".getBytes(java.nio.charset.StandardCharsets.UTF_8))).compact();
        assertEquals(401, postConAcceso("/auth/verification-status", cuerpo, vencido).statusCode());
        String acceso = ticket();
        usuario.setCredencialesVersion(usuario.getCredencialesVersion() + 1); usuarios.saveAndFlush(usuario);
        assertEquals(401, postConAcceso("/auth/verification-status", cuerpo, acceso).statusCode());
    }

    @Test void cambioDeCorreoBloqueaElCodigoTrasCincoIntentos() {
        String anterior = usuario.getEmail();
        String nuevo = UUID.randomUUID() + "@example.com";
        var datos = new com.gg.turnlook.features.usuario.dto.UsuarioModificarDTO();
        datos.setEmail(nuevo);
        assertThrows(BadRequestException.class, () -> usuarioService.modificarUsuario(datos, usuario.getId(), anterior));
        cambioCorreo.solicitar(anterior, nuevo);
        var codigo = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(email).enviarCodigoCambioCorreo(eq(nuevo), codigo.capture());
        String incorrecto = codigo.getValue().equals("123456") ? "654321" : "123456";
        for (int intento = 0; intento < 5; intento++) {
            assertThrows(BadRequestException.class, () -> cambioCorreo.confirmar(anterior, nuevo, incorrecto));
        }
        assertThrows(BadRequestException.class, () -> cambioCorreo.confirmar(anterior, nuevo, codigo.getValue()));
        assertEquals(anterior, usuarios.findById(usuario.getId()).orElseThrow().getEmail());
        verify(email, never()).enviarAvisoCambioCorreo(anyString());
    }
}
