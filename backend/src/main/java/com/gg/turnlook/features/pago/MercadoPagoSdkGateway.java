package com.gg.turnlook.features.pago;

import com.gg.turnlook.features.servicio.Servicio;
import com.gg.turnlook.features.turno.Turno;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MercadoPagoSdkGateway implements MercadoPagoGateway {

    private final String accessToken;

    public MercadoPagoSdkGateway(@Value("${mercadopago.access-token}") String accessToken) {
        this.accessToken = accessToken;
    }

    @Override
    public String crearPreferencia(Turno turno) throws MPException, MPApiException {
        MercadoPagoConfig.setAccessToken(accessToken);

        Servicio servicio = turno.getServicio();
        PreferenceItemRequest item = PreferenceItemRequest.builder()
                .title(servicio.getNombre())
                .description(servicio.getDescripcion())
                .quantity(1)
                .currencyId("ARS")
                .unitPrice(servicio.getPrecio())
                .build();

        PreferenceRequest request = PreferenceRequest.builder()
                .items(List.of(item))
                .externalReference(turno.getId().toString())
                .notificationUrl("https://appointmee-vcs2.onrender.com/pagos/webhook")
                .build();

        Preference preference = new PreferenceClient().create(request);
        return preference.getInitPoint();
    }

    @Override
    public PagoMercadoPago obtenerPago(Long pagoId) throws MPException, MPApiException {
        MercadoPagoConfig.setAccessToken(accessToken);
        Payment payment = new PaymentClient().get(pagoId);
        return new PagoMercadoPago(payment.getStatus(), payment.getExternalReference());
    }
}
