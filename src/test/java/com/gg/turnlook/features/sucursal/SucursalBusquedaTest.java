package com.gg.turnlook.features.sucursal;

import com.gg.turnlook.features.sucursal.categoria.Categoria;
import com.gg.turnlook.features.sucursal.categoria.ECategoria;
import jakarta.persistence.EntityManager;
import java.time.LocalTime;
import java.util.List;
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

    @Test
    void persisteUbicacionYConsultaSucursalesSinUbicacion() {
        Sucursal conUbicacion = guardar("Sucursal con ubicacion", true, ECategoria.BARBERIA);
        conUbicacion.setLatitud(-34.6037);
        conUbicacion.setLongitud(-58.3816);
        conUbicacion.setGooglePlaceId("ChIJ_ubicada");
        repository.saveAndFlush(conUbicacion);
        guardar("Sucursal vieja sin ubicacion", true, ECategoria.BARBERIA);
        entityManager.clear();

        List<Sucursal> activas = repository.findByActivoTrue();

        Sucursal cargada = activas.stream()
                .filter(s -> s.getId().equals(conUbicacion.getId())).findFirst().orElseThrow();
        assertEquals(-34.6037, cargada.getLatitud());
        assertEquals(-58.3816, cargada.getLongitud());
        assertEquals("ChIJ_ubicada", cargada.getGooglePlaceId());

        Sucursal vieja = activas.stream()
                .filter(s -> "Sucursal vieja sin ubicacion".equals(s.getNombre()))
                .findFirst().orElseThrow();
        assertNull(vieja.getLatitud());
        assertNull(vieja.getLongitud());
        assertNull(vieja.getGooglePlaceId());
    }

    @Test
    void existeGooglePlaceIdExcluyendoLaPropiaSucursal() {
        Sucursal sucursal = guardar("Sucursal con place id", true, ECategoria.BARBERIA);
        sucursal.setGooglePlaceId("ChIJ_unico");
        repository.saveAndFlush(sucursal);

        assertTrue(repository.existsByActivoTrueAndGooglePlaceId("ChIJ_unico"));
        assertFalse(repository.existsByActivoTrueAndGooglePlaceId("ChIJ_otro"));
        assertTrue(repository.existsByActivoTrueAndGooglePlaceIdAndIdNot("ChIJ_unico", 0));
        assertFalse(repository.existsByActivoTrueAndGooglePlaceIdAndIdNot("ChIJ_unico", sucursal.getId()));
    }

    @Test
    void laUnicidadDeGooglePlaceIdIgnoraSucursalesInactivas() {
        Sucursal activa = guardar("Sucursal activa con place id", true, ECategoria.BARBERIA);
        activa.setGooglePlaceId("ChIJ_activo");
        repository.saveAndFlush(activa);

        Sucursal inactiva = guardar("Sucursal inactiva con place id", false, ECategoria.BARBERIA);
        inactiva.setGooglePlaceId("ChIJ_liberado");
        repository.saveAndFlush(inactiva);

        assertFalse(repository.existsByActivoTrueAndGooglePlaceId("ChIJ_liberado"));
        assertTrue(repository.existsByActivoTrueAndGooglePlaceId("ChIJ_activo"));
        assertFalse(repository.existsByActivoTrueAndGooglePlaceIdAndIdNot("ChIJ_activo", activa.getId()));
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
