package com.gg.turnlook.Backend.Controllers;


import com.gg.turnlook.Backend.DTO.Imagen.ImagenSubidaDTO;
import com.gg.turnlook.Backend.Enum.ImagenCloudinaryTipo;
import com.gg.turnlook.Backend.Service.ImagenCloudinaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;



@RestController
@RequestMapping("/imagenes")
public class ImagenController {

    private final ImagenCloudinaryService imagenCloudinaryService;


    public ImagenController(ImagenCloudinaryService imagenCloudinaryService) {
        this.imagenCloudinaryService = imagenCloudinaryService;
    }


    /// ENDPOINTS


    @PreAuthorize("isAuthenticated()")
    @PostMapping("/upload")
    public ResponseEntity<ImagenSubidaDTO> subir(
            @RequestParam("file") MultipartFile archivo,
            @RequestParam ImagenCloudinaryTipo tipo
    ) {

        return ResponseEntity.ok(imagenCloudinaryService.subir(archivo, tipo));
    }


    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<String> datosInvalidos(Exception error) {
        return ResponseEntity.badRequest().body("Los datos de la imagen son invalidos o estan incompletos");
    }
}
