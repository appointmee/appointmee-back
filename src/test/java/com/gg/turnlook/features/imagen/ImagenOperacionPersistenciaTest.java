package com.gg.turnlook.features.imagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.gg.turnlook.common.exception.BadRequestException;
import com.gg.turnlook.features.usuario.Usuario;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class ImagenOperacionPersistenciaTest {

    @Autowired ImagenService service;
    @Autowired ImagenRepository repository;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean ImagenCloudinaryService cloudinary;

    private Integer imagenId;
    private Integer usuarioId;
    private final String anterior = "appointmee/perfiles/anterior";

    @BeforeEach
    void preparar() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Imagen imagen = new Imagen();
            imagen.setUrl("https://res.cloudinary.com/cloud/image/upload/" + anterior + ".jpg");
            imagen.setPublicId(anterior);
            repository.saveAndFlush(imagen);
            imagenId = imagen.getId();
            Usuario usuario = new Usuario("Ana", "Pérez", UUID.randomUUID() + "@example.com", "clave");
            usuario.setFotoPerfil(imagen);
            entityManager.persist(usuario);
            entityManager.flush();
            usuarioId = usuario.getId();
        });
        when(cloudinary.esImagenPropia(anyString())).thenReturn(true);
        doThrow(new BadRequestException("Cloudinary no responde")).when(cloudinary).eliminar(anterior);
    }

    @AfterEach
    void limpiar() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            entityManager.remove(entityManager.find(Usuario.class, usuarioId));
            entityManager.flush();
            repository.deleteById(imagenId);
        });
    }

    @Test
    void conservaLaFotoAnteriorSiCloudinaryFallaAlReemplazar() {
        Usuario usuario = new Usuario();
        usuario.setFotoPerfil(repository.findById(imagenId).orElseThrow());

        assertThrows(BadRequestException.class, () -> service.cambiarFotoPerfilUsuario(usuario,
                "https://res.cloudinary.com/cloud/image/upload/appointmee/perfiles/nueva.jpg",
                "appointmee/perfiles/nueva"));

        assertEquals(anterior, repository.findById(imagenId).orElseThrow().getPublicId());
    }

    @Test
    void conservaLaReferenciaSiCloudinaryFallaAlBorrarElPerfil() {
        Usuario usuario = new Usuario();
        usuario.setFotoPerfil(repository.findById(imagenId).orElseThrow());

        assertThrows(BadRequestException.class, () -> service.eliminarFotoPerfilUsuario(usuario));

        assertEquals(anterior, repository.findById(imagenId).orElseThrow().getPublicId());
    }

    @Test
    void unBorradoFallidoConservaLaFilaParaReintentar() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            entityManager.find(Usuario.class, usuarioId).setFotoPerfil(null);
            entityManager.flush();
        });

        assertThrows(BadRequestException.class,
                () -> service.eliminarImagen(repository.findById(imagenId).orElseThrow()));

        assertTrue(repository.existsById(imagenId));
    }
}
