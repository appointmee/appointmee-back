package com.gg.turnlook.auth.password;

import com.gg.turnlook.features.verificacion.canalEnvio.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class PasswordChangedListener {
    private final EmailService email;

    // Se avisa solo cuando el cambio quedó confirmado en la base de datos.
    @TransactionalEventListener
    public void avisar(PasswordChangedEvent event) {
        try {
            email.enviarAvisoCambioPassword(event.email());
        } catch (RestClientException exception) {
            // Una caída del correo no revierte una contraseña ya actualizada.
            LoggerFactory.getLogger(PasswordChangedListener.class)
                    .warn("No se pudo enviar el aviso de cambio de contraseña");
        }
    }
}
