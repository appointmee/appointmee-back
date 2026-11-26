package com.gg.turnlook.features.notificacion;

import com.gg.turnlook.common.exception.BadRequestException;
import com.gg.turnlook.common.exception.NotFoundException;
import com.gg.turnlook.common.exception.UnauthorizedException;
import com.gg.turnlook.features.notificacion.dto.ContadorNoLeidasDTO;
import com.gg.turnlook.features.notificacion.dto.NotificacionDTO;
import com.gg.turnlook.features.notificacion.dto.PaginaNotificacionesDTO;
import com.gg.turnlook.features.usuario.Usuario;
import com.gg.turnlook.features.usuario.UsuarioRepository;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de negocio de las notificaciones in-app.
 *
 * <p>Las lecturas se hacen siempre con el email del usuario autenticado.
 * La creación es interna: la invocan los módulos que generan eventos
 * (turnos, pagos, etc.), nunca el cliente a través de un endpoint público.</p>
 */
@Service
public class NotificacionService {

    public static final int TAMANO_DEFECTO = 20;
    public static final int TAMANO_MAXIMO = 50;

    private static final int LONGITUD_TITULO = 120;
    private static final int LONGITUD_MENSAJE = 500;

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;

    public NotificacionService(NotificacionRepository notificacionRepository,
                               UsuarioRepository usuarioRepository) {
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Devuelve una página de notificaciones propias, opcionalmente filtradas por estado. */
    @Transactional(readOnly = true)
    public PaginaNotificacionesDTO listar(String email, Boolean leida, int pagina, int tamano) {
        String autenticado = emailAutenticado(email);
        Pageable paginacion = PageRequest.of(Math.max(pagina, 0), tamanoValido(tamano));

        Page<Notificacion> resultado =
                notificacionRepository.listarPorUsuario(autenticado, leida, paginacion);

        return new PaginaNotificacionesDTO(
                resultado.getContent().stream().map(this::aDTO).toList(),
                resultado.getNumber(),
                resultado.getSize(),
                resultado.getTotalElements(),
                resultado.getTotalPages(),
                notificacionRepository.contarNoLeidas(autenticado));
    }

    /** Contador de no leídas del usuario autenticado, calculado en la base. */
    @Transactional(readOnly = true)
    public ContadorNoLeidasDTO contarNoLeidas(String email) {
        return new ContadorNoLeidasDTO(
                notificacionRepository.contarNoLeidas(emailAutenticado(email)));
    }

    /** Crea una notificación buscando al destinatario por email (uso interno). */
    @Transactional
    public NotificacionDTO registrar(String email, TipoNotificacion tipo,
                                     String titulo, String mensaje) {
        String destinatario = emailDestino(email);
        Usuario usuario = usuarioRepository.findByEmailAndActivoTrue(destinatario)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        return registrar(usuario, tipo, titulo, mensaje);
    }

    /** Crea una notificación para un usuario ya resuelto (uso interno). */
    @Transactional
    public NotificacionDTO registrar(Usuario usuario, TipoNotificacion tipo,
                                     String titulo, String mensaje) {
        validar(usuario, tipo, titulo, mensaje);

        Notificacion notificacion = new Notificacion();
        notificacion.setUsuario(usuario);
        notificacion.setTipo(tipo);
        notificacion.setTitulo(titulo.trim());
        notificacion.setMensaje(mensaje.trim());
        notificacion.setLeida(false);

        return aDTO(notificacionRepository.save(notificacion));
    }

    private NotificacionDTO aDTO(Notificacion notificacion) {
        return new NotificacionDTO(
                notificacion.getId(),
                notificacion.getTipo(),
                notificacion.getTitulo(),
                notificacion.getMensaje(),
                notificacion.getFechaCreacion(),
                notificacion.isLeida());
    }

    private void validar(Usuario usuario, TipoNotificacion tipo, String titulo, String mensaje) {
        if (usuario == null || usuario.getId() == null) {
            throw new BadRequestException("La notificación debe pertenecer a un usuario");
        }
        if (tipo == null) {
            throw new BadRequestException("El tipo de notificación es obligatorio");
        }
        if (titulo == null || titulo.isBlank()) {
            throw new BadRequestException("El título de la notificación es obligatorio");
        }
        if (titulo.trim().length() > LONGITUD_TITULO) {
            throw new BadRequestException(
                    "El título no puede superar los " + LONGITUD_TITULO + " caracteres");
        }
        if (mensaje == null || mensaje.isBlank()) {
            throw new BadRequestException("El mensaje de la notificación es obligatorio");
        }
        if (mensaje.trim().length() > LONGITUD_MENSAJE) {
            throw new BadRequestException(
                    "El mensaje no puede superar los " + LONGITUD_MENSAJE + " caracteres");
        }
    }

    private int tamanoValido(int tamano) {
        if (tamano <= 0) {
            return TAMANO_DEFECTO;
        }
        return Math.min(tamano, TAMANO_MAXIMO);
    }

    private String emailAutenticado(String email) {
        if (email == null || email.isBlank()) {
            throw new UnauthorizedException("No autenticado");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String emailDestino(String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("El email del destinatario es obligatorio");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
