package com.gg.turnlook.Backend.Controllers;


import com.gg.turnlook.Backend.DTO.SolicitudEmpleador.SolicitudEmpleadorCrearDTO;
import com.gg.turnlook.Backend.Service.SolicitudEmpleadorService;
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


    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @GetMapping("/listar")
    public ResponseEntity<?> listarSolicitudesEmpleador() {

        return ResponseEntity.ok().body(solicitudEmpleadorService.listarSolicitudesPendientes());
    }


    @PreAuthorize("hasRole('CLIENTE')")
    @GetMapping("/propias")
    public ResponseEntity<?> listarSolicitudesEmpleadorPropias(
            @AuthenticationPrincipal String userEmail) {

        return ResponseEntity.ok().body(
                solicitudEmpleadorService.listarSolicitudesPropias(userEmail));
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



