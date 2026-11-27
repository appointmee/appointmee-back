package com.gg.turnlook.features.payment.records;

import com.gg.turnlook.features.payment.PaymentState;


/** Estado consultado al proveedor, ya traducido al lenguaje del dominio. */
public record PagoProveedor(
        String proveedorReferencia,
        String externalReference,
        PaymentState estado) {
}
