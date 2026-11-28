package com.gg.turnlook.features.mercadopago.api;

/** Cliente HTTP del proveedor. Aísla el REST del adaptador de dominio. */
public interface MercadoPagoApi {

    PreferenciaResponse crearPreferencia(PreferenciaRequest request, String accessToken,
                                         String idempotencyKey);

    PagoApiResponse obtenerPago(String paymentId, String accessToken);
}
