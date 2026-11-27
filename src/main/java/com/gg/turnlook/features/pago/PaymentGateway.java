package com.gg.turnlook.features.pago;

/**
 * Puerto de cobro. La lógica de negocio depende de este contrato, nunca de
 * Mercado Pago ni de su SDK.
 */
public interface PaymentGateway {

    CobroCreado crearCobro(CrearCobroCommand command);

    EstadoPagoProveedor consultarEstado(String proveedorReferencia);
}
