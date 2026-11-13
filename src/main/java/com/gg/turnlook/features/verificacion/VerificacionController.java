package com.gg.turnlook.features.verificacion;
import com.gg.turnlook.features.verificacion.VerificacionService;
import com.gg.turnlook.features.verificacion.dto.SolicitarCodigoRequest;
import com.gg.turnlook.features.verificacion.dto.VerificarCodigoRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/verificaciones")
@RequiredArgsConstructor
public class VerificacionController {

    private final VerificacionService verificacionService;

    @PostMapping("/solicitar-codigo")
    public ResponseEntity<Void> solicitarCodigo(
            @Valid @RequestBody SolicitarCodigoRequest request) {

        verificacionService.solicitarCodigo(request);

        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/verificar-codigo")
    public ResponseEntity<Void> verificarCodigo(
            @Valid @RequestBody VerificarCodigoRequest request) {

        verificacionService.verificarCodigo(request);

        return ResponseEntity.ok().build();
    }
}