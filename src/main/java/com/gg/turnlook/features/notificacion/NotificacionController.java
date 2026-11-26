package com.gg.turnlook.features.notificacion;

import com.gg.turnlook.features.notificacion.dto.ContadorNoLeidasDTO;
import com.gg.turnlook.features.notificacion.dto.PaginaNotificacionesDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta de notificaciones del usuario autenticado.
 *
 * <p>No hay endpoints de creación: las notificaciones las generan los módulos
 * de negocio mediante {@link NotificacionService}. El JWT es obligatorio según
 * la configuración global de seguridad; el email llega como principal.</p>
 */
@RestController
@RequestMapping("/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping
    public ResponseEntity<PaginaNotificacionesDTO> listar(
            @AuthenticationPrincipal String email,
            @RequestParam(name = "leida", required = false) Boolean leida,
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestParam(name = "tamano", defaultValue = "20") int tamano) {

        return ResponseEntity.ok(notificacionService.listar(email, leida, pagina, tamano));
    }

    @GetMapping("/no-leidas")
    public ResponseEntity<ContadorNoLeidasDTO> contarNoLeidas(
            @AuthenticationPrincipal String email) {

        return ResponseEntity.ok(notificacionService.contarNoLeidas(email));
    }
}
