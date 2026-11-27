package com.gg.turnlook.features.mercadopago;

import com.gg.turnlook.features.mercadopago.api.MercadoPagoApi;
import com.gg.turnlook.features.mercadopago.api.PagoApiResponse;
import com.gg.turnlook.features.mercadopago.api.PreferenciaRequest;
import com.gg.turnlook.features.mercadopago.api.PreferenciaResponse;
import com.gg.turnlook.features.pago.Pago;
import com.gg.turnlook.features.payment.PaymentGateway;
import com.gg.turnlook.features.payment.PaymentProviderException;
import com.gg.turnlook.features.payment.comandos.CrearCobroCommand;
import com.gg.turnlook.features.payment.records.CobroCreado;
import com.gg.turnlook.features.payment.records.PagoProveedor;


import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Adaptador de Mercado Pago para el prototipo académico. Usa el token de la
 * aplicación (cobro a nombre de AppointMee), nunca OAuth de vendedor.
 */
@Component
public class MercadoPagoSandboxAdapter implements PaymentGateway {

    private final MercadoPagoApi api;
    private final MercadoPagoProperties properties;

    public MercadoPagoSandboxAdapter(MercadoPagoApi api, MercadoPagoProperties properties) {
        this.api = api;
        this.properties = properties;
    }

    @Override
    public CobroCreado crearCobro(CrearCobroCommand command) {
        PreferenciaRequest request = new PreferenciaRequest(
                List.of(new PreferenciaRequest.Item(
                        "Cobro turno " + command.pagoId(),
                        command.descripcion(),
                        1,
                        command.moneda(),
                        command.monto())),
                String.valueOf(command.pagoId()),
                properties.getNotificationUrl(),
                new PreferenciaRequest.Payer(command.emailPagador()));

        try {
            PreferenciaResponse response = api.crearPreferencia(
                    request, properties.getAccessToken(), command.idempotencyKey());
            return new CobroCreado(
                    response.id(),
                    response.initPoint(),
                    request.externalReference(),
                    EstadoPagoProveedor.desdeProveedor(response.status(), null).aPaymentState());
        } catch (RuntimeException e) {
            throw new PaymentProviderException("No se pudo crear el cobro en Mercado Pago", e);
        }
    }

    @Override
    public PagoProveedor consultarPago(String proveedorReferencia) {
        try {
            PagoApiResponse response = api.obtenerPago(
                    proveedorReferencia, properties.getAccessToken());
            return new PagoProveedor(
                    response.id(),
                    response.externalReference(),
                    EstadoPagoProveedor
                            .desdeProveedor(response.status(), response.statusDetail())
                            .aPaymentState());
        } catch (RuntimeException e) {
            throw new PaymentProviderException("No se pudo consultar el cobro en Mercado Pago", e);
        }
    }
}
