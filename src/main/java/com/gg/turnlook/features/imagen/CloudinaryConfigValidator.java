package com.gg.turnlook.features.imagen;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Valida al arrancar la aplicación que las credenciales de Cloudinary
 * estén presentes. Si faltan, falla el inicio con un mensaje claro
 * en lugar de fallar silenciosamente en la primera subida o eliminación.
 */
@Component
public class CloudinaryConfigValidator implements ApplicationRunner {

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    @Override
    public void run(ApplicationArguments args) {
        if (cloudName == null || cloudName.isBlank()
                || apiKey == null || apiKey.isBlank()
                || apiSecret == null || apiSecret.isBlank()) {
            throw new IllegalStateException(
                    "Falta configurar Cloudinary. Define las variables de entorno: "
                            + "CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY y CLOUDINARY_API_SECRET");
        }
    }
}
