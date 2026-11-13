package com.gg.turnlook.features.usuario;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios/cambio-email")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class CambioCorreoController {
    private final CambioCorreoService cambios;

    @PostMapping("/solicitar")
    public ResponseEntity<Void> solicitar(@AuthenticationPrincipal String actual,
            @Valid @RequestBody Solicitud solicitud) {
        cambios.solicitar(actual, solicitud.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/confirmar")
    public ResponseEntity<Void> confirmar(@AuthenticationPrincipal String actual,
            @Valid @RequestBody Confirmacion confirmacion) {
        cambios.confirmar(actual, confirmacion.email(), confirmacion.codigo());
        return ResponseEntity.noContent().build();
    }

    public record Solicitud(
            @NotBlank @Email @Size(min = 8, max = 150) String email) {}

    public record Confirmacion(
            @NotBlank @Email @Size(min = 8, max = 150) String email,
            @NotBlank @Pattern(regexp = "[0-9]{6}") String codigo) {}
}
