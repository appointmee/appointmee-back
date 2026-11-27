package com.gg.turnlook.features.usuario;

import com.gg.turnlook.features.imagen.Imagen;
import com.gg.turnlook.features.usuario.rol.ERol;
import com.gg.turnlook.features.usuario.rol.Rol;
import com.gg.turnlook.features.usuario.rol.RolRepository;
import jakarta.persistence.EntityManager;
import java.util.HashSet;
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
    @Autowired private RolRepository rolRepo;
    @Autowired private EntityManager entityManager;

    @Test
    void listaUsuariosSinFiltrosNulos() {
        Usuario activo = guardar("Lucía", "García", "lucia@example.com", true,
                "https://res.cloudinary.com/example/lucia.jpg");
        Usuario inactivo = guardar("Mario", "Paz", "mario@example.com", false);

        var resultados = repository.buscarMiniAdmin(
                null, null, null, null, false, null, PageRequest.of(0, 50));

        assertEquals(Set.of(activo.getId(), inactivo.getId()), resultados.stream()
                .map(fila -> (Integer) fila[0])
                .collect(Collectors.toSet()));
        assertEquals("https://res.cloudinary.com/example/lucia.jpg", resultados.getFirst()[4]);
        assertNull(resultados.getLast()[4]);
    }

    @Test
    void combinaFiltrosDeNombreApellidoYEstado() {
        Usuario esperado = guardar("Lucía", "García", "lucia@example.com", true);
        guardar("Lucía", "García", "lucia-inactiva@example.com", false);
        guardar("Lucía", "Paz", "lucia-paz@example.com", true);

        var resultados = repository.buscarMiniAdmin(
                "lUcÍa", "gArCía", true, null, false, null, PageRequest.of(0, 50));

        assertEquals(1, resultados.size());
        assertEquals(esperado.getId(), resultados.getFirst()[0]);
    }

    @Test
    void buscaNombreYApellidoPorCoincidenciaParcialSinDistinguirMayusculas() {
        Usuario giovanni = guardar("Giovanni", "Marcos", "giovanni@example.com", true);
        Usuario giovanna = guardar("Giovanna", "María", "giovanna@example.com", true);
        Usuario gio = guardar("Gio", "Mariano", "gio@example.com", true);
        Usuario giovv = guardar("giovv", "Marta", "giovv@example.com", true);
        guardar("Ana", "García", "ana@example.com", true);
        guardar("Mario", "Pérez", "mario@example.com", true);

        var resultados = repository.buscarMiniAdmin(
                "gIo", "mAr", null, null, false, null, PageRequest.of(0, 50));

        assertEquals(Set.of(giovanni.getId(), giovanna.getId(), gio.getId(), giovv.getId()),
                resultados.stream()
                        .map(fila -> (Integer) fila[0])
                        .collect(Collectors.toSet()));
    }

    @Test
    void filtraPorEmailYRolSinDuplicarUsuariosConVariosRoles() {
        Rol cliente = rolRepo.saveAndFlush(new Rol(null, ERol.CLIENTE, new HashSet<>()));
        Rol empleado = rolRepo.saveAndFlush(new Rol(null, ERol.EMPLEADO, new HashSet<>()));

        Usuario esperado = guardar("Lucía", "García", "lucia@example.com", true);
        esperado.getRoles().add(cliente);
        esperado.getRoles().add(empleado);
        repository.saveAndFlush(esperado);

        Usuario otro = guardar("Mario", "Paz", "mario@example.com", true);
        otro.getRoles().add(empleado);
        repository.saveAndFlush(otro);

        var porEmail = repository.buscarMiniAdmin(
                null, null, null, "@EXAMPLE.com", false, null, PageRequest.of(0, 50));
        assertEquals(2, porEmail.size());

        var porRol = repository.buscarMiniAdmin(
                null, null, null, null, true, ERol.CLIENTE, PageRequest.of(0, 50));
        assertEquals(1, porRol.size());
        assertEquals(esperado.getId(), porRol.getFirst()[0]);
    }

    @Test
    void combinaNombreApellidoEmailEstadoYRolConCoincidenciasParciales() {
        Rol cliente = rolRepo.saveAndFlush(new Rol(null, ERol.CLIENTE, new HashSet<>()));
        Rol empleado = rolRepo.saveAndFlush(new Rol(null, ERol.EMPLEADO, new HashSet<>()));

        Usuario esperado = guardar("Lucía", "García", "lucia@example.com", true);
        esperado.getRoles().add(cliente);
        repository.saveAndFlush(esperado);

        Usuario inactivo = guardar("Lucía", "García", "lucia-inactiva@example.com", false);
        inactivo.getRoles().add(cliente);
        repository.saveAndFlush(inactivo);

        Usuario otroRol = guardar("Lucía", "García", "lucia-empleado@example.com", true);
        otroRol.getRoles().add(empleado);
        repository.saveAndFlush(otroRol);

        Usuario otroNombre = guardar("Lucas", "García", "lucas@example.com", true);
        otroNombre.getRoles().add(cliente);
        repository.saveAndFlush(otroNombre);

        var resultados = repository.buscarMiniAdmin(
                "ucÍ", "arC", true, "LUCIA@", true, ERol.CLIENTE,
                PageRequest.of(0, 50));

        assertEquals(1, resultados.size());
        assertEquals(esperado.getId(), resultados.getFirst()[0]);
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
