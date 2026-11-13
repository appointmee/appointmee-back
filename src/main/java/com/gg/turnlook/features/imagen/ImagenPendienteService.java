package com.gg.turnlook.features.imagen;

import com.gg.turnlook.common.exception.BadRequestException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ImagenPendienteService {

    private static final long VIGENCIA_SEGUNDOS = 30 * 60;

    private final ImagenRepository imagenRepository;
    private final ImagenCloudinaryService cloudinaryService;
    private final byte[] clave;

    public ImagenPendienteService(
            ImagenRepository imagenRepository,
            ImagenCloudinaryService cloudinaryService,
            @Value("${cloudinary.api-secret:}") String apiSecret
    ) {
        this.imagenRepository = imagenRepository;
        this.cloudinaryService = cloudinaryService;
        this.clave = apiSecret.getBytes(StandardCharsets.UTF_8);
    }

    public String crearToken(String publicId, String usuario) {
        if (clave.length == 0 || !cloudinaryService.esImagenPropia(publicId)
                || usuario == null || usuario.isBlank()) {
            throw new BadRequestException("No se pudo autorizar la imagen subida");
        }
        long vencimiento = Instant.now().getEpochSecond() + VIGENCIA_SEGUNDOS;
        return vencimiento + "." + firmar(publicId, usuario, vencimiento);
    }

    public void descartar(String publicId, String token, String usuario) {
        if (token == null || usuario == null || !cloudinaryService.esImagenPropia(publicId)) {
            throw new BadRequestException("No se puede descartar esta imagen");
        }
        int separador = token.indexOf('.');
        if (separador < 1 || separador == token.length() - 1) {
            throw new BadRequestException("La autorización de la imagen es inválida");
        }

        try {
            long vencimiento = Long.parseLong(token.substring(0, separador));
            byte[] recibida = Base64.getUrlDecoder().decode(token.substring(separador + 1));
            byte[] esperada = Base64.getUrlDecoder().decode(firmar(publicId, usuario, vencimiento));
            if (vencimiento < Instant.now().getEpochSecond()
                    || !MessageDigest.isEqual(recibida, esperada)) {
                throw new BadRequestException("La autorización de la imagen venció o es inválida");
            }
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("La autorización de la imagen es inválida");
        }

        if (imagenRepository.contarEnUsoPorPublicId(publicId) == 0) {
            cloudinaryService.eliminar(publicId);
            imagenRepository.eliminarHuerfanasPorPublicId(publicId);
        }
    }

    private String firmar(String publicId, String usuario, long vencimiento) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clave, "HmacSHA256"));
            String contenido = vencimiento + "\n" + usuario + "\n" + publicId;
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    mac.doFinal(contenido.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar la autorización de la imagen", e);
        }
    }
}
