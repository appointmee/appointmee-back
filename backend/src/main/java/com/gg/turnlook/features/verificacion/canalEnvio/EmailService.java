package com.gg.turnlook.features.verificacion.canalEnvio;

import com.gg.turnlook.features.verificacion.CanalEnvioService;
import com.gg.turnlook.features.verificacion.enums.CanalVerificacion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.Map;

@Service
public class EmailService implements CanalEnvioService {

    private final RestClient cliente;
    private final String remitente;

    public EmailService(
            @Value("${resend.api-key}") String apiKey,
            @Value("${resend.from}") String remitente) {

        this.remitente = remitente;

        this.cliente = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Override
    public CanalVerificacion getCanal() {
        return CanalVerificacion.EMAIL;
    }

    @Override
    public void enviarCodigo(String destinatario, String codigo) {
        cliente.post()
                .uri("/emails")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "from", remitente,
                        "to", List.of(destinatario),
                        "subject", "Verificación de email - Appointme",
                        "text", "Tu código de verificación es: " + codigo
                                + "\nVence en 5 minutos."
                                + "\nSi no lo solicitaste, ignorá este correo."
                ))
                .retrieve()
                .toBodilessEntity();
    }
    public void enviarRecuperacion(String destinatario, String enlace, long minutos) {
        String html = """
                <!doctype html>
                <html lang="es">
                <body style="margin:0;padding:32px 16px;background:#f5f5f5;font-family:Arial,sans-serif;color:#202020;">
                  <main style="max-width:480px;margin:0 auto;padding:32px;background:#ffffff;border-radius:16px;">
                    <p style="font-size:18px;font-weight:bold;">AppointMee</p>
                    <h1 style="font-size:24px;">Restablecé tu contraseña</h1>
                    <p style="line-height:1.6;">Recibimos una solicitud para cambiar la contraseña de tu cuenta.</p>
                    <p style="margin:28px 0;">
                      <a href="%s" style="display:inline-block;padding:14px 24px;border-radius:8px;background:#f7c65a;color:#17130a;font-weight:bold;text-decoration:none;">Restablecer contraseña</a>
                    </p>
                    <p style="line-height:1.6;">El botón permite cambiar tu contraseña una sola vez y vence en %d minutos.</p>
                    <p style="color:#666666;line-height:1.6;">Si no solicitaste este cambio, ignorá este correo.</p>
                  </main>
                </body>
                </html>
                """.formatted(HtmlUtils.htmlEscape(enlace), minutos);

        cliente.post().uri("/emails").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("from", remitente, "to", List.of(destinatario),
                        "subject", "Recuperar contraseña - Appointme",
                        "html", html,
                        "text", "Restablecé tu contraseña: " + enlace + "\nVence en " + minutos
                                + " minutos. Si no lo solicitaste, ignorá este correo."))
                .retrieve().toBodilessEntity();
    }

    public void enviarAvisoCambioPassword(String destinatario) {
        cliente.post().uri("/emails").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("from", remitente, "to", List.of(destinatario),
                        "subject", "Tu contraseña se actualizó - AppointMee",
                        "text", "La contraseña de tu cuenta de AppointMee se actualizó correctamente."
                                + "\nCerramos tus sesiones anteriores. Volvé a iniciar sesión con tu nueva contraseña."
                                + "\nSi no realizaste este cambio, recuperá tu cuenta desde la aplicación y contactá al soporte."))
                .retrieve().toBodilessEntity();
    }
}
