package com.gg.turnlook.Backend.Controllers;


import com.gg.turnlook.Backend.Service.SolicitudEmpleadoService;
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


    @PreAuthorize("hasRole('EMPLEADOR')")
    @GetMapping("/enviadas/sucursal/{sucursalId}")
    public ResponseEntity<?> listarSolicitudesEnviadasPorSucursal(
            @PathVariable("sucursalId") Integer sucursalId,
            @AuthenticationPrincipal String empleadorEmail) {

        return ResponseEntity.ok().body(
                solicitudEmpleadoService.listarSolicitudesEnviadas(sucursalId, empleadorEmail));
    }


    @PreAuthorize("hasRole('CLIENTE')")
    @GetMapping("/recibidas")
    public ResponseEntity<?> listarSolicitudesRecibidas(
            @AuthenticationPrincipal String userEmail) {

        return ResponseEntity.ok().body(
                solicitudEmpleadoService.listarSolicitudesRecibidas(userEmail));
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




