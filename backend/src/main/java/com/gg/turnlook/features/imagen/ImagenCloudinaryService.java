package com.gg.turnlook.features.imagen;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.gg.turnlook.common.exception.BadRequestException;
import com.gg.turnlook.features.imagen.dto.ImagenSubidaDTO;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Locale;
import java.net.URI;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImagenCloudinaryService {

    private static final String CARPETA_RAIZ = "appointmee/";
    private static final long TAMANIO_MAXIMO = 5 * 1024 * 1024;

    private final Cloudinary cloudinary;
    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;

    public ImagenCloudinaryService(
            Cloudinary cloudinary,
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret
    ) {
        this.cloudinary = cloudinary;
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    public ImagenSubidaDTO subir(MultipartFile archivo, ImagenCloudinaryTipo tipo) {
        validarConfiguracion();
        validarArchivo(archivo);
        if (tipo == null) {
            throw new BadRequestException("Debe indicar el tipo de imagen");
        }

        try {
            byte[] contenido = archivo.getBytes();
            String carpeta = CARPETA_RAIZ + tipo.getCarpeta();
            Map<?, ?> resultado = cloudinary.uploader().upload(
                    contenido,
                    ObjectUtils.asMap(
                            // folder mantiene compatibilidad con cuentas legacy; asset_folder
                            // organiza cuentas dynamic y la opción conserva el prefijo del public_id.
                            "folder", carpeta,
                            "asset_folder", carpeta,
                            "use_asset_folder_as_public_id_prefix", true,
                            "resource_type", "image",
                            "allowed_formats", Arrays.asList("jpg", "jpeg", "png", "webp", "gif"),
                            "max_file_size", TAMANIO_MAXIMO
                    )
            );

            Object url = resultado.get("secure_url");
            Object publicId = resultado.get("public_id");
            if (!(publicId instanceof String id) || !esImagenPropia(id)) {
                throw new BadRequestException("Cloudinary devolvió datos incompletos para la imagen");
            }
            String urlSegura = url instanceof String valor ? valor : null;
            try {
                validarReferencia(urlSegura, id);
            } catch (BadRequestException error) {
                try { eliminar(id); }
                catch (RuntimeException limpieza) { error.addSuppressed(limpieza); }
                throw error;
            }

            return new ImagenSubidaDTO(
                    urlSegura,
                    id,
                    archivo.getOriginalFilename()
            );
        } catch (BadRequestException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new BadRequestException("No se pudo subir la imagen a Cloudinary");
        }
    }

    public void eliminar(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        if (!esImagenPropia(publicId)) {
            throw new BadRequestException("La imagen no pertenece a AppointMee");
        }

        validarConfiguracion();
        try {
            Map<?, ?> resultado = cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap("resource_type", "image", "invalidate", true)
            );
            Object estado = resultado.get("result");
            if (!"ok".equals(estado) && !"not found".equals(estado)) {
                throw new BadRequestException("Cloudinary no pudo eliminar la imagen");
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof BadRequestException badRequestException) {
                throw badRequestException;
            }
            throw new BadRequestException("No se pudo eliminar la imagen de Cloudinary");
        }
    }

    public boolean esImagenPropia(String publicId) {
        return publicId != null && publicId.length() <= 255
                && publicId.matches("appointmee/(perfiles|sucursales|servicios|galerias)/[A-Za-z0-9_/-]+")
                && !publicId.endsWith("/") && !publicId.contains("//");
    }

    public void validarReferencia(String url, String publicId) {
        if (url == null || !esImagenPropia(publicId)) {
            throw new BadRequestException("La referencia de Cloudinary es inválida");
        }
        try {
            URI uri = URI.create(url);
            String ruta = "/" + cloudName + "/image/upload/";
            String patron = Pattern.quote(ruta) + "(?:v[0-9]+/)?"
                    + Pattern.quote(publicId) + "\\.(?:jpg|jpeg|png|webp|gif)";
            if (!esImagenPropia(publicId) || url.length() > 1000
                    || !"https".equals(uri.getScheme()) || !"res.cloudinary.com".equals(uri.getHost())
                    || uri.getPort() != -1 || uri.getUserInfo() != null
                    || uri.getQuery() != null || uri.getFragment() != null
                    || !uri.getPath().matches(patron)) {
                throw new BadRequestException("La URL no corresponde a la imagen de Cloudinary indicada");
            }
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("La URL de Cloudinary es inválida");
        }
    }

    private void validarConfiguracion() {
        if (cloudName == null || cloudName.isBlank()
                || apiKey == null || apiKey.isBlank()
                || apiSecret == null || apiSecret.isBlank()) {
            throw new BadRequestException("Falta configurar Cloudinary");
        }
    }

    private void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BadRequestException("Debe seleccionar una imagen");
        }
        if (archivo.getSize() > TAMANIO_MAXIMO) {
            throw new BadRequestException("La imagen no puede superar los 5 MB");
        }

        String tipoContenido = archivo.getContentType();
        if (tipoContenido == null || !tipoContenido.matches("(?i)image/(jpeg|png|webp|gif)")) {
            throw new BadRequestException("El archivo debe ser una imagen JPG, PNG, WEBP o GIF");
        }

        try {
            byte[] contenido = archivo.getBytes();
            if (!coincideFormato(contenido, tipoContenido.toLowerCase(Locale.ROOT))) {
                throw new BadRequestException("El contenido del archivo no coincide con su formato");
            }
        } catch (IOException e) {
            throw new BadRequestException("No se pudo leer el archivo de imagen");
        }
    }

    private boolean coincideFormato(byte[] bytes, String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> bytes.length >= 3
                    && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8
                    && (bytes[2] & 0xFF) == 0xFF;
            case "image/png" -> bytes.length >= 8
                    && Arrays.equals(Arrays.copyOf(bytes, 8),
                    new byte[]{(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10});
            case "image/gif" -> bytes.length >= 6
                    && (new String(bytes, 0, 6, java.nio.charset.StandardCharsets.US_ASCII)
                    .equals("GIF87a") || new String(bytes, 0, 6,
                    java.nio.charset.StandardCharsets.US_ASCII).equals("GIF89a"));
            case "image/webp" -> bytes.length >= 12
                    && new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
                    && new String(bytes, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP");
            default -> false;
        };
    }
}
