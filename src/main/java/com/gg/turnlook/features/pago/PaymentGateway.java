package com.gg.turnlook.features.pago;

import com.gg.turnlook.features.pago.comandos.CrearCobroCommand;
import com.gg.turnlook.features.pago.records.CobroCreado;
import com.gg.turnlook.features.pago.records.PagoProveedor;

/**
 * Puerto de cobro. La lógica de negocio depende de este contrato, nunca de
 * Mercado Pago ni de su SDK. Los estados ya vienen traducidos a PaymentState.
 */
public interface PaymentGateway {

    CobroCreado crearCobro(CrearCobroCommand command);

    PagoProveedor consultarPago(String proveedorReferencia);
}
