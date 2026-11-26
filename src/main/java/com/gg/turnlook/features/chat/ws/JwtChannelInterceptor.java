package com.gg.turnlook.features.chat.ws;

import com.gg.turnlook.auth.jwt.JwtService;
import com.gg.turnlook.features.chat.ChatAccesoService;
import com.gg.turnlook.features.usuario.Usuario;
import com.gg.turnlook.features.usuario.UsuarioRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Autentica el frame CONNECT con el JWT y valida la participación antes de
 * permitir SUBSCRIBE o SEND en un canal de conversación.
 */
@AllArgsConstructor
@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    static final String ATRIBUTO_EMAIL = "chat.email";
    private static final Pattern DESTINO_CONVERSACION =
            Pattern.compile("/(?:app|topic)/conversaciones/(\\d+)");
    private static final String PREFIJO_NOTIFICACIONES = "/topic/notificaciones/";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepo;
    private final ChatAccesoService acceso;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand comando = accessor.getCommand();
        if (comando == null) {
            return message;
        }

        if (comando == StompCommand.CONNECT) {
            return autenticar(message, accessor);
        }

        if (comando != StompCommand.SUBSCRIBE && comando != StompCommand.SEND) {
            return message;
        }
        String email = emailDe(accessor);
        if (email == null) {
                throw new AccessDeniedException("Sesión WebSocket no autenticada");
        }
        String destino = accessor.getDestination();
        Integer conversacionId = extraerConversacionId(destino);
        if (conversacionId != null && !acceso.esParticipante(conversacionId, email)) {
                throw new AccessDeniedException("No sos participante de esta conversación");
        }
        validarDestinoNotificaciones(destino, email);

        return message;
    }

    private Message<?> autenticar(Message<?> message, StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new AccessDeniedException("Falta el token en el frame CONNECT");
        }

        String token = header.substring(7).trim();
        Claims claims;
        try {
            claims = jwtService.validarAccessToken(token);
        } catch (JwtException | IllegalArgumentException e) {
            throw new AccessDeniedException("Token inválido o vencido");
        }

        String email = claims.getSubject();
        Usuario usuario = usuarioRepo.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new AccessDeniedException("Usuario no disponible"));
        if (!usuario.isEmailVerificado()) {
            throw new AccessDeniedException("Email no verificado");
        }
        if (!usuario.getId().equals(claims.get("id", Integer.class))
                || !Integer.valueOf(usuario.getCredencialesVersion())
                        .equals(claims.get("version", Integer.class))) {
            throw new AccessDeniedException("Sesión revocada");
        }

        // Spring 7 no propaga un mapa reemplazado con setSessionAttributes al resto
        // de los frames: hay que mutar el mapa de la sesión que viene en el mensaje.
        Map<String, Object> atributos = accessor.getSessionAttributes();
        if (atributos == null) {
            atributos = new HashMap<>();
            accessor.setSessionAttributes(atributos);
        }
        atributos.put(ATRIBUTO_EMAIL, email);
        accessor.setUser(new UsernamePasswordAuthenticationToken(email, null, List.of()));

        return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
    }

    private String emailDe(StompHeaderAccessor accessor) {
        if (accessor.getUser() != null) {
            return accessor.getUser().getName();
        }
        Map<String, Object> atributos = accessor.getSessionAttributes();
        return atributos == null ? null : (String) atributos.get(ATRIBUTO_EMAIL);
    }

    /** Cada usuario sólo puede usar su propio canal privado de notificaciones. */
    private void validarDestinoNotificaciones(String destino, String email) {
        if (destino == null || !destino.startsWith(PREFIJO_NOTIFICACIONES)) {
            return;
        }
        String propio = PREFIJO_NOTIFICACIONES + email.trim().toLowerCase(Locale.ROOT);
        if (!propio.equals(destino)) {
            throw new AccessDeniedException("No podés usar el canal de notificaciones de otro usuario");
        }
    }

    private Integer extraerConversacionId(String destino) {
        if (destino == null) {
            return null;
        }
        Matcher matcher = DESTINO_CONVERSACION.matcher(destino);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }
}
