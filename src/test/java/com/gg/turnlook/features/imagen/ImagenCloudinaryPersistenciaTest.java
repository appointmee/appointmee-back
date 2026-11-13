package com.gg.turnlook.features.imagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.gg.turnlook.features.usuario.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class ImagenCloudinaryPersistenciaTest {

    @Autowired
    private ImagenRepository imagenRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @Transactional
    void persisteLaUrlYElPublicIdDeCloudinary() {
        Imagen imagen = new Imagen();
        imagen.setUrl("https://res.cloudinary.com/cloud/image/upload/appointmee/perfiles/foto.jpg");
        imagen.setPublicId("appointmee/perfiles/foto");

        Imagen guardada = imagenRepository.saveAndFlush(imagen);
        entityManager.clear();

        Imagen recuperada = imagenRepository.findById(guardada.getId()).orElseThrow();
        assertEquals(imagen.getUrl(), recuperada.getUrl());
        assertEquals(imagen.getPublicId(), recuperada.getPublicId());
    }

    @Test
    @Transactional
    void distingueUnaImagenHuerfanaDeUnaFotoDePerfilEnUso() {
        Imagen imagen = new Imagen();
        imagen.setUrl("https://res.cloudinary.com/cloud/foto.jpg");
        imagen.setPublicId("appointmee/perfiles/foto");
        Imagen guardada = imagenRepository.saveAndFlush(imagen);

        assertEquals(0, imagenRepository.contarEnUsoPorPublicId(imagen.getPublicId()));

        Usuario usuario = new Usuario("Ana", "Pérez", "ana@example.com", "clave");
        usuario.setFotoPerfil(guardada);
        entityManager.persist(usuario);
        entityManager.flush();

        assertEquals(1, imagenRepository.contarEnUsoPorPublicId(imagen.getPublicId()));
        assertEquals(0, imagenRepository.eliminarHuerfanasPorPublicId(imagen.getPublicId()));
    }

    @Test
    @Transactional
    void eliminaLaFilaHuerfanaLuegoDeDescartarSuArchivo() {
        Imagen imagen = new Imagen();
        imagen.setUrl("https://res.cloudinary.com/cloud/huerfana.jpg");
        imagen.setPublicId("appointmee/perfiles/huerfana");
        imagenRepository.saveAndFlush(imagen);

        assertEquals(1, imagenRepository.eliminarHuerfanasPorPublicId(imagen.getPublicId()));
        assertFalse(imagenRepository.existsById(imagen.getId()));
    }
}
