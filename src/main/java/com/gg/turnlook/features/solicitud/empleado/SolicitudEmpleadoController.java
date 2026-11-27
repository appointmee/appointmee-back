package com.gg.turnlook.features.solicitud.empleado;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;




@RestController
@RequestMapping("/solicitudes-empleado")
@CrossOrigin(origins = "*")
public class SolicitudEmpleadoController {


    private final SolicitudEmpleadoService solicitudEmpleadoService;


    public SolicitudEmpleadoController(SolicitudEmpleadoService solicitudEmpleadoService) {
        this.solicitudEmpleadoService = solicitudEmpleadoService;
    }


    /// ENDPOINTS


    /** Vista previa de 3 invitaciones enviadas desde la sucursal. */
    @PreAuthorize("hasRole('EMPLEADOR')")
    @GetMapping("/enviadas/sucursal/{sucursalId}")
    public ResponseEntity<?> listarSolicitudesEnviadasPorSucursal(
            @PathVariable("sucursalId") Integer sucursalId,
            @AuthenticationPrincipal String empleadorEmail) {

        return ResponseEntity.ok().body(
                solicitudEmpleadoService.listarSolicitudesEnviadas(sucursalId, empleadorEmail));
    }

    /** Historial completo: 10 invitaciones por pagina, solo las enviadas. */
    @PreAuthorize("hasRole('EMPLEADOR')")
    @GetMapping("/enviadas/sucursal/{sucursalId}/paginado")
    public ResponseEntity<?> listarSolicitudesEnviadasPorSucursalPaginadas(
            @PathVariable("sucursalId") Integer sucursalId,
            @AuthenticationPrincipal String empleadorEmail,
            @RequestParam(name = "page", defaultValue = "0") String page) {

        return ResponseEntity.ok().body(
                solicitudEmpleadoService.listarSolicitudesEnviadasPaginadas(
                        sucursalId, empleadorEmail, page));
    }


    @PreAuthorize("hasRole('CLIENTE')")
    @GetMapping("/recibidas")
    public ResponseEntity<?> listarSolicitudesRecibidas(
            @AuthenticationPrincipal String userEmail) {

        return ResponseEntity.ok().body(
                solicitudEmpleadoService.listarSolicitudesRecibidas(userEmail));
    }

    /** Historial completo: 10 solicitudes por pagina, solo las recibidas. */
    @PreAuthorize("hasRole('CLIENTE')")
    @GetMapping("/recibidas/paginado")
    public ResponseEntity<?> listarSolicitudesRecibidasPaginadas(
            @AuthenticationPrincipal String userEmail,
            @RequestParam(name = "page", defaultValue = "0") String page) {

        return ResponseEntity.ok().body(
                solicitudEmpleadoService.listarSolicitudesRecibidasPaginadas(userEmail, page));
    }


    @GetMapping("/{solicitudId}/detalles")
    public ResponseEntity<?> verDetallesSolicitud(
            @PathVariable("solicitudId") Integer solicitudId,
            @AuthenticationPrincipal String userEmail) {

        return ResponseEntity.ok().body(
                solicitudEmpleadoService.verDetallesSolicitud(solicitudId, userEmail));
    }


    @PreAuthorize("hasRole('CLIENTE')")
    @PatchMapping("/{solicitudId}/aprobar")
    public ResponseEntity<?> aprobarSolicitud(
            @PathVariable("solicitudId") Integer solicitudId,
            @AuthenticationPrincipal String userEmail) {

        solicitudEmpleadoService.aceptarSolicitud(solicitudId, userEmail);
        return ResponseEntity.ok().body("Se aceptó la solicitud");
    }


    @PreAuthorize("hasRole('CLIENTE')")
    @PatchMapping("/{solicitudId}/rechazar")
    public ResponseEntity<?> rechazarSolicitud(
            @PathVariable("solicitudId") Integer solicitudId,
            @AuthenticationPrincipal String userEmail) {

        solicitudEmpleadoService.rechazarSolicitud(solicitudId, userEmail);
        return ResponseEntity.ok().body("Se rechazó la solicitud");
    }


}




