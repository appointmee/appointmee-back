package com.gg.turnlook.features.imagen;

import com.gg.turnlook.features.imagen.dto.ImagenCancelacionDTO;
import com.gg.turnlook.features.imagen.dto.ImagenSubidaDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/imagenes")
public class ImagenController {

    private final ImagenCloudinaryService imagenCloudinaryService;
    private final ImagenPendienteService imagenPendienteService;

    public ImagenController(ImagenCloudinaryService imagenCloudinaryService,
                            ImagenPendienteService imagenPendienteService) {
        this.imagenCloudinaryService = imagenCloudinaryService;
        this.imagenPendienteService = imagenPendienteService;
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/upload")
    public ResponseEntity<ImagenSubidaDTO> subir(
            @RequestParam("file") MultipartFile archivo,
            @RequestParam ImagenCloudinaryTipo tipo,
            @AuthenticationPrincipal String usuario
    ) {
        ImagenSubidaDTO subida = imagenCloudinaryService.subir(archivo, tipo);
        try {
            String token = imagenPendienteService.crearToken(subida.publicId(), usuario);
            return ResponseEntity.ok(new ImagenSubidaDTO(
                    subida.url(), subida.publicId(), subida.nombreArchivo(), token));
        } catch (RuntimeException error) {
            try { imagenCloudinaryService.eliminar(subida.publicId()); }
            catch (RuntimeException limpieza) { error.addSuppressed(limpieza); }
            throw error;
        }
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/upload")
    public ResponseEntity<Void> descartar(
            @RequestBody ImagenCancelacionDTO imagen,
            @AuthenticationPrincipal String usuario
    ) {
        imagenPendienteService.descartar(imagen.publicId(), imagen.tokenCancelacion(), usuario);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<String> datosInvalidos(Exception error) {
        return ResponseEntity.badRequest().body("Los datos de la imagen son inválidos o están incompletos");
    }
}
