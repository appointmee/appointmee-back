package com.gg.turnlook.features.imagen;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gg.turnlook.features.imagen.dto.ImagenSubidaDTO;
import com.gg.turnlook.features.sucursal.Sucursal;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

class ImagenGaleriaFalloTest {

    @Test
    void eliminaDeCloudinarySiFallaElGuardadoDeLaGaleria() {
        ImagenRepository repository = mock(ImagenRepository.class);
        ImagenCloudinaryService cloudinary = mock(ImagenCloudinaryService.class);
        MultipartFile archivo = mock(MultipartFile.class);
        when(cloudinary.subir(archivo, ImagenCloudinaryTipo.GALERIA))
                .thenReturn(new ImagenSubidaDTO("https://res.cloudinary.com/cloud/foto.jpg",
                        "appointmee/galerias/foto", "foto.jpg"));
        when(repository.saveAndFlush(any(Imagen.class))).thenThrow(new IllegalStateException("Falló la base"));

        ImagenService service = new ImagenService(repository, cloudinary);

        assertThrows(IllegalStateException.class,
                () -> service.subirImagenSucursal(mock(Sucursal.class), archivo));
        verify(cloudinary).eliminar("appointmee/galerias/foto");
    }
}
