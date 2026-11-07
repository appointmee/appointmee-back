package com.gg.turnlook.features.pago;

import com.gg.turnlook.features.turno.Turno;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;

public interface MercadoPagoGateway {
    String crearPreferencia(Turno turno) throws MPException, MPApiException;

    PagoMercadoPago obtenerPago(Long pagoId) throws MPException, MPApiException;
}
