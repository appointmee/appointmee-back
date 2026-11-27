package com.gg.turnlook.features.solicitud.empleador;

import com.gg.turnlook.features.solicitud.empleador.dto.PaginaSolicitudesEmpleadorDTO;
import com.gg.turnlook.features.solicitud.empleador.dto.SolicitudEmpleadorCrearDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;




@RestController
@RequestMapping("/solicitudes-empleador")
@CrossOrigin(origins = "*")
public class SolicitudEmpleadorController {


    private final SolicitudEmpleadorService solicitudEmpleadorService;


    public SolicitudEmpleadorController(SolicitudEmpleadorService solicitudEmpleadorService) {
        this.solicitudEmpleadorService = solicitudEmpleadorService;
    }


    /// ENDPOINTS


    @PreAuthorize("!hasAnyRole('ADMINISTRADOR', 'EMPLEADOR')")
    @PostMapping("/solicitar")
    public ResponseEntity<?> solicitarSerEmpleador(
            @Valid @RequestBody SolicitudEmpleadorCrearDTO solicitudEmpleador,
            @AuthenticationPrincipal String userEmail) {

        solicitudEmpleadorService.registrarSolicitud(solicitudEmpleador, userEmail);
        return ResponseEntity.ok().body("Se registró tu solicitud");
    }


    /// Listado administrativo paginado. estado es opcional; page empieza en 0
    /// y size por defecto es 10. Los parametros se validan en el servicio.
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @GetMapping("/listar")
    public ResponseEntity<PaginaSolicitudesEmpleadorDTO> listarSolicitudesEmpleador(
            @RequestParam(value = "estado", required = false) String estado,
            @RequestParam(value = "page", defaultValue = "0") String page,
            @RequestParam(value = "size", defaultValue = "10") String size) {

        return ResponseEntity.ok().body(
                solicitudEmpleadorService.listarSolicitudesAdministrativas(estado, page, size));
    }


    @PreAuthorize("hasRole('CLIENTE')")
    @GetMapping("/propias")
    public ResponseEntity<?> listarSolicitudesEmpleadorPropias(
            @AuthenticationPrincipal String userEmail) {

        return ResponseEntity.ok().body(
                solicitudEmpleadorService.listarSolicitudesPropias(userEmail));
    }

    /** Historial completo: 10 solicitudes por pagina, solo las enviadas por el usuario. */
    @PreAuthorize("hasRole('CLIENTE')")
    @GetMapping("/propias/paginado")
    public ResponseEntity<?> listarSolicitudesEmpleadorPropiasPaginadas(
            @AuthenticationPrincipal String userEmail,
            @RequestParam(name = "page", defaultValue = "0") String page) {

        return ResponseEntity.ok().body(
                solicitudEmpleadorService.listarSolicitudesPropiasPaginadas(userEmail, page));
    }


    @GetMapping("/{solicitudId}/detalles")
    public ResponseEntity<?> verDetallesSolicitud(
            @PathVariable("solicitudId") Integer solicitudId,
            @AuthenticationPrincipal String userEmail) {

        return  ResponseEntity.ok().body(
                solicitudEmpleadorService.verDetallesSolicitud(solicitudId, userEmail));
    }


    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PatchMapping("/{solicitudId}/aprobar")
    public ResponseEntity<?> aprobarSolicitudEmpleador(
            @PathVariable("solicitudId") Integer solicitudId) {

        solicitudEmpleadorService.aprobarSolicitud(solicitudId);
        return ResponseEntity.ok().body("Se aprobó la solicitud");
    }


    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PatchMapping("/{solicitudId}/rechazar")
    public ResponseEntity<?> rechazarSolicitudEmpleador(
            @PathVariable("solicitudId") Integer solicitudId) {

        solicitudEmpleadorService.rechazarSolicitud(solicitudId);
        return ResponseEntity.ok().body("Se rechazó la solicitud");
    }


}
