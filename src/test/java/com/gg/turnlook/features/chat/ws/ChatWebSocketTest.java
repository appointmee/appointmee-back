package com.gg.turnlook.features.chat.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gg.turnlook.auth.jwt.JwtService;
import com.gg.turnlook.features.chat.ChatAccesoService;
import com.gg.turnlook.features.chat.MensajeService;
import com.gg.turnlook.features.chat.dto.MensajeCrearDTO;
import com.gg.turnlook.features.chat.dto.MensajeResponseDTO;
import com.gg.turnlook.features.usuario.Usuario;
import com.gg.turnlook.features.usuario.UsuarioRepository;
import io.jsonwebtoken.Claims;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class ChatWebSocketTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final UsuarioRepository usuarioRepo = mock(UsuarioRepository.class);
    private final ChatAccesoService acceso = mock(ChatAccesoService.class);
    private final MensajeService mensajeService = mock(MensajeService.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final MessageChannel canal = mock(MessageChannel.class);

    private JwtChannelInterceptor interceptor;
    private ChatWebSocketController controller;
    private final Principal principal = () -> "cliente@example.com";

    @BeforeEach
    void setUp() {
        interceptor = new JwtChannelInterceptor(jwtService, usuarioRepo, acceso);
        controller = new ChatWebSocketController(mensajeService, messagingTemplate);
    }

    private Message<byte[]> mensaje(StompHeaderAccessor accessor) {
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private StompHeaderAccessor conectado() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/conversaciones/50/mensajes");
        accessor.setUser(new UsernamePasswordAuthenticationToken("cliente@example.com", null, List.of()));
        return accessor;
    }

    private void jwtValido() {
        Claims claims = mock(Claims.class);
        when(jwtService.validarAccessToken("token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("cliente@example.com");
        when(claims.get("id", Integer.class)).thenReturn(1);
        when(claims.get("version", Integer.class)).thenReturn(0);

        Usuario usuario = mock(Usuario.class);
        when(usuario.getId()).thenReturn(1);
        when(usuario.isEmailVerificado()).thenReturn(true);
        when(usuario.getCredencialesVersion()).thenReturn(0);
        when(usuarioRepo.findByEmailAndActivoTrue("cliente@example.com"))
                .thenReturn(Optional.of(usuario));
    }

    @Test
    void connectConJwtValidoAsignaUsuarioYSesion() {
        jwtValido();
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer token");

        Message<?> resultado = interceptor.preSend(mensaje(accessor), canal);

        assertNotNull(resultado);
        StompHeaderAccessor salida = StompHeaderAccessor.wrap(resultado);
        assertEquals("cliente@example.com", salida.getUser().getName());
        assertEquals("cliente@example.com",
                salida.getSessionAttributes().get(JwtChannelInterceptor.ATRIBUTO_EMAIL));
    }

    @Test
    void connectSinTokenSeRechaza() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);

        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(mensaje(accessor), canal));
    }

    @Test
    void sendSinAutenticarSeRechaza() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/conversaciones/50/mensajes");

        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(mensaje(accessor), canal));
    }

    @Test
    void sendDeNoParticipanteSeRechaza() {
        when(acceso.esParticipante(50, "cliente@example.com")).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(mensaje(conectado()), canal));
    }

    @Test
    void subscribeDeParticipantePasa() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/conversaciones/50");
        accessor.setUser(new UsernamePasswordAuthenticationToken("cliente@example.com", null, List.of()));
        when(acceso.esParticipante(50, "cliente@example.com")).thenReturn(true);

        assertNotNull(interceptor.preSend(mensaje(accessor), canal));
    }

    @Test
    void enviarPersisteYReemiteAlTopic() {
        MensajeCrearDTO dto = new MensajeCrearDTO("hola", UUID.randomUUID());
        MensajeResponseDTO esperado = new MensajeResponseDTO(
                7L, 50, 1, "Cliente Uno", "hola", null, null, dto.getClienteMensajeId());
        when(mensajeService.enviar(50, dto, "cliente@example.com")).thenReturn(esperado);

        controller.enviar(50, dto, principal);

        verify(mensajeService).enviar(50, dto, "cliente@example.com");
        verify(messagingTemplate).convertAndSend("/topic/conversaciones/50", esperado);
    }

    @Test
    void ackReemiteElEstadoActualizado() {
        MensajeResponseDTO esperado = new MensajeResponseDTO(
                7L, 50, 4, "Empleado Cuatro", "hola", null, null, UUID.randomUUID());
        when(mensajeService.marcarEntregado(50, 7L, "cliente@example.com")).thenReturn(esperado);

        controller.entregado(50, 7L, principal);

        verify(mensajeService).marcarEntregado(50, 7L, "cliente@example.com");
        verify(messagingTemplate).convertAndSend("/topic/conversaciones/50", esperado);
        verify(messagingTemplate, never()).convertAndSend("/topic/conversaciones/51", esperado);
    }
}
