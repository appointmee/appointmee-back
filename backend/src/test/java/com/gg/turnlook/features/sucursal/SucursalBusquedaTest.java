package com.gg.turnlook.features.sucursal;

import com.gg.turnlook.features.sucursal.categoria.Categoria;
import com.gg.turnlook.features.sucursal.categoria.ECategoria;
import jakarta.persistence.EntityManager;
import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class SucursalBusquedaTest {
    @Autowired private SucursalRepository repository;
    @Autowired private EntityManager entityManager;

    @Test
    void buscaConOSinEspaciosSinDistinguirMayusculas() {
        Sucursal separada = guardar("Barber House", true, ECategoria.BARBERIA);
        Sucursal junta = guardar("BarberHouse Norte", true, ECategoria.BARBERIA);
        guardar("Otra sucursal", true, ECategoria.BARBERIA);
        guardar("Barber House cerrada", false, ECategoria.BARBERIA);

        for (String busqueda : new String[]{"barberhouse", "Barber House", "  BARBER  HOUSE  ", "barberhou"}) {
            Set<Integer> encontrados = repository.findByNombreContainingIgnoreCaseAndActivoTrue(busqueda)
                    .stream().map(Sucursal::getId).collect(Collectors.toSet());
            assertEquals(Set.of(separada.getId(), junta.getId()), encontrados);
        }
    }

    @Test
    void mantieneElFiltroDeCategoriaYExcluyeLasInactivas() {
        Sucursal barberia = guardar("Barber House", true, ECategoria.BARBERIA);
        guardar("BarberHouse Spa", true, ECategoria.SPA);
        guardar("Barber House cerrada", false, ECategoria.BARBERIA);

        var encontrados = repository.findByNombreContainingIgnoreCaseAndCategoriaCategoriaAndActivoTrue(
                "barberhouse", ECategoria.BARBERIA);
        assertEquals(1, encontrados.size());
        assertEquals(barberia.getId(), encontrados.getFirst().getId());
    }

    @Test
    void porcentajesYGuionesBajosSeBuscanLiteralmente() {
        guardar("Barber House", true, ECategoria.BARBERIA);
        assertTrue(repository.findByNombreContainingIgnoreCaseAndActivoTrue("barber%").isEmpty());
        assertTrue(repository.findByNombreContainingIgnoreCaseAndActivoTrue("barber_").isEmpty());
    }

    private Sucursal guardar(String nombre, boolean activo, ECategoria tipo) {
        Categoria categoria = new Categoria(tipo);
        entityManager.persist(categoria);
        Sucursal sucursal = new Sucursal();
        sucursal.setNombre(nombre);
        sucursal.setDireccion(nombre);
        sucursal.setDescripcion("Sucursal de prueba");
        sucursal.setHoraApertura(LocalTime.of(9, 0));
        sucursal.setHoraCierre(LocalTime.of(18, 0));
        sucursal.setActivo(activo);
        sucursal.setCategoria(categoria);
        return repository.saveAndFlush(sucursal);
    }
}
