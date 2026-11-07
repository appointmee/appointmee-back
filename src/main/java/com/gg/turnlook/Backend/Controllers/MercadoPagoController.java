package com.gg.turnlook.Backend.Controllers;


import com.gg.turnlook.Backend.Service.MercadoPagoService;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/pagos")
public class MercadoPagoController {


    private final MercadoPagoService mpService;


    public MercadoPagoController(MercadoPagoService mpService) {
        this.mpService = mpService;
    }



    /// ENDPOINTS



    @PreAuthorize("hasRole('CLIENTE')")
    @PostMapping("/{turnoId}")
    public ResponseEntity<?> realizarPago(@PathVariable("turnoId") Integer turnoId,
                                          @AuthenticationPrincipal String clienteEmail
    ) throws MPException, MPApiException {

        try {
            return ResponseEntity.ok().body(mpService.pagar(turnoId, clienteEmail));
        } catch (MPException e1) {
            return ResponseEntity.status(500).body("Error mp1");  // test
        } catch (MPApiException e2) {
            return ResponseEntity.status(500).body("Error mp2");  // test
        }
    }


    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String id) throws MPException, MPApiException {

        if(id == null){
            return ResponseEntity.ok().build();
        }

        if(!"payment".equals(topic)){
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.ok().body(mpService.procesarWebhook(
                Long.valueOf(id)));
    }


}
