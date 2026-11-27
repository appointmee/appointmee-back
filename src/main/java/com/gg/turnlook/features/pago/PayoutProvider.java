package com.gg.turnlook.features.pago;

/**
 * Puerto de liquidación. En la tesis se implementa con un adapter simulado;
 * el dominio no sabe si el dinero se mueve realmente o no.
 */
public interface PayoutProvider {

    PayoutResult pay(PayoutCommand command);

    PayoutStatus getStatus(String payoutReference);

    PayoutResult refund(RefundCommand command);
}
