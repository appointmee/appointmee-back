package com.gg.turnlook.features.estadisticas;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/graficos")
@RequiredArgsConstructor
public class EstadisticasController {
    private final EstadisticasService estadisticasService;

    @PreAuthorize("hasRole('EMPLEADOR')")
    @GetMapping("/empleador")
    public ResponseEntity<EstadisticasResponseDTO> obtenerGraficos(
            @RequestParam Integer sucursalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "DIA") String agrupacion,
            @AuthenticationPrincipal String empleadorEmail) {
        return ResponseEntity.ok(estadisticasService.obtener(
                sucursalId, empleadorEmail, desde, hasta, agrupacion));
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @GetMapping("/administrador")
    public ResponseEntity<EstadisticasResponseDTO.Admin> obtenerGraficosAdmin(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "DIA") String agrupacion) {
        return ResponseEntity.ok(estadisticasService.obtenerParaAdmin(desde, hasta, agrupacion));
    }
}
