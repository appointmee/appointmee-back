package com.gg.turnlook.features.payment;

import com.gg.turnlook.features.pago.Pago;
import com.gg.turnlook.features.payment.comandos.CrearCobroCommand;
import com.gg.turnlook.features.payment.records.CobroCreado;
import com.gg.turnlook.features.payment.records.PagoProveedor;


/**
 * Puerto de cobro. La lógica de negocio depende de este contrato, nunca de
 * Mercado Pago ni de su SDK. Los estados ya vienen traducidos a PaymentState.
 */
public interface PaymentGateway {

    CobroCreado crearCobro(CrearCobroCommand command);

    PagoProveedor consultarPago(String proveedorReferencia);
}
