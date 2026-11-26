package com.gg.turnlook.features.notificacion;

import com.gg.turnlook.features.notificacion.dto.ContadorNoLeidasDTO;
import com.gg.turnlook.features.notificacion.dto.PaginaNotificacionesDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta y marcado de lectura de las notificaciones del usuario autenticado.
 *
 * <p>No hay endpoints de creación: las notificaciones las generan los módulos
 * de negocio mediante {@link NotificacionService}. El JWT es obligatorio según
 * la configuración global de seguridad; el email llega como principal y toda
 * escritura queda acotada a las notificaciones propias.</p>
 */
@RestController
@RequestMapping("/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @GetMapping
    public ResponseEntity<PaginaNotificacionesDTO> listar(
            @AuthenticationPrincipal String email,
            @RequestParam(name = "tipo", required = false) TipoNotificacion tipo,
            @RequestParam(name = "leida", required = false) Boolean leida,
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestParam(name = "tamano", defaultValue = "20") int tamano) {

        return ResponseEntity.ok(notificacionService.listar(email, tipo, leida, pagina, tamano));
    }

    @GetMapping("/no-leidas")
    public ResponseEntity<ContadorNoLeidasDTO> contarNoLeidas(
            @AuthenticationPrincipal String email) {

        return ResponseEntity.ok(notificacionService.contarNoLeidas(email));
    }

    /** Marca una notificación propia como leída y devuelve el contador actualizado. */
    @PatchMapping("/{id}/leido")
    public ResponseEntity<ContadorNoLeidasDTO> marcarLeida(
            @PathVariable("id") Integer id,
            @AuthenticationPrincipal String email) {

        return ResponseEntity.ok(notificacionService.marcarLeida(email, id));
    }

    /** Marca todas las notificaciones propias como leídas; admite el filtro de tipo. */
    @PatchMapping("/leer-todas")
    public ResponseEntity<ContadorNoLeidasDTO> marcarTodasLeidas(
            @AuthenticationPrincipal String email,
            @RequestParam(name = "tipo", required = false) TipoNotificacion tipo) {

        return ResponseEntity.ok(notificacionService.marcarTodasLeidas(email, tipo));
    }
}
