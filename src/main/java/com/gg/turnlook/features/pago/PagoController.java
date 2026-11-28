package com.gg.turnlook.features.pago;

import com.gg.turnlook.features.pago.dto.PagoResponseDTO;
import com.gg.turnlook.features.pago.dto.PayoutRequestDTO;
import com.gg.turnlook.features.pago.dto.RefundRequestDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API REST del flujo C. No modifica el contrato heredado POST /pagos/{turnoId}. */
@RestController
@RequestMapping("/pagos")
@Tag(name = "Pagos", description = "Consulta, liquidación y reembolso de cobros centralizados")
public class PagoController {

    private final PagoApiService service;

    public PagoController(PagoApiService service) {
        this.service = service;
    }

    @GetMapping("/mios")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar pagos propios o de las sucursales del usuario")
    public List<PagoResponseDTO> mios(@AuthenticationPrincipal String email) {
        return service.listarMios(email);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Detalle de un pago, con estado financiero y turno asociado")
    public PagoResponseDTO detalle(@PathVariable Integer id, @AuthenticationPrincipal String email) {
        return service.detalle(id, email);
    }

    @PostMapping("/{id}/payout")
    @PreAuthorize("hasAnyRole('EMPLEADOR','ADMINISTRADOR')")
    @Operation(summary = "Liquidar un pago a la sucursal acreedora")
    public PagoResponseDTO liquidar(@PathVariable Integer id,
                                    @AuthenticationPrincipal String email,
                                    @Valid @RequestBody PayoutRequestDTO request) {
        return service.liquidar(id, email, request);
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasAnyRole('EMPLEADOR','ADMINISTRADOR')")
    @Operation(summary = "Reembolsar total o parcialmente un pago liquidado")
    public PagoResponseDTO reembolsar(@PathVariable Integer id,
                                      @AuthenticationPrincipal String email,
                                      @Valid @RequestBody RefundRequestDTO request) {
        return service.reembolsar(id, email, request);
    }
}
