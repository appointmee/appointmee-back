package com.gg.turnlook.features.usuario;

import com.gg.turnlook.features.imagen.Imagen;
import com.gg.turnlook.features.usuario.dto.UsuarioMiniAdminDTO;
import jakarta.persistence.EntityManager;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class UsuarioRepositoryAdminTest {
    @Autowired private UsuarioRepository repository;
    @Autowired private EntityManager entityManager;

    @Test
    void listaUsuariosSinFiltrosNulos() {
        Usuario activo = guardar("Lucía", "García", "lucia@example.com", true,
                "https://res.cloudinary.com/example/lucia.jpg");
        Usuario inactivo = guardar("Mario", "Paz", "mario@example.com", false);

        var resultados = repository.buscarMiniAdmin(null, null, null, PageRequest.of(0, 50));

        assertEquals(Set.of(activo.getId(), inactivo.getId()), resultados.stream()
                .map(UsuarioMiniAdminDTO::getId)
                .collect(Collectors.toSet()));
        assertEquals("https://res.cloudinary.com/example/lucia.jpg", resultados.getFirst().getFotoPerfil());
        assertNull(resultados.getLast().getFotoPerfil());
    }

    @Test
    void combinaFiltrosDeNombreApellidoYEstado() {
        Usuario esperado = guardar("Lucía", "García", "lucia@example.com", true);
        guardar("Lucía", "García", "lucia-inactiva@example.com", false);
        guardar("Lucía", "Paz", "lucia-paz@example.com", true);

        var resultados = repository.buscarMiniAdmin("lUcÍa", "gArCía", true, PageRequest.of(0, 50));

        assertEquals(1, resultados.size());
        assertEquals(esperado.getId(), resultados.getFirst().getId());
    }

    private Usuario guardar(String nombre, String apellido, String email, boolean activo) {
        return guardar(nombre, apellido, email, activo, null);
    }

    private Usuario guardar(String nombre, String apellido, String email, boolean activo, String fotoUrl) {
        Usuario usuario = new Usuario(nombre, apellido, email, "hash");
        usuario.setActivo(activo);
        if (fotoUrl != null) {
            Imagen imagen = new Imagen();
            imagen.setUrl(fotoUrl);
            entityManager.persist(imagen);
            usuario.setFotoPerfil(imagen);
        }
        return repository.saveAndFlush(usuario);
    }
}
