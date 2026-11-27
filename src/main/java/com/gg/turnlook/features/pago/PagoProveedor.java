package com.gg.turnlook.features.pago;

/** Estado consultado al proveedor, ya traducido al lenguaje del dominio. */
public record PagoProveedor(
        String proveedorReferencia,
        String externalReference,
        PaymentState estado) {
}
