package com.gg.turnlook.features.pago;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Configuración del adaptador. El token nunca se expone al frontend. */
@Component
@ConfigurationProperties(prefix = "app.pagos.mercadopago")
public class MercadoPagoProperties {

    private String apiUrl = "https://api.mercadopago.com";
    private String notificationUrl = "http://localhost:8080/pagos/webhook";
    private String accessToken;

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getNotificationUrl() {
        return notificationUrl;
    }

    public void setNotificationUrl(String notificationUrl) {
        this.notificationUrl = notificationUrl;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
