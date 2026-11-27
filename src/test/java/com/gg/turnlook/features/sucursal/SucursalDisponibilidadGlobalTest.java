package com.gg.turnlook.features.sucursal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gg.turnlook.features.servicio.Servicio;
import com.gg.turnlook.features.sucursal.categoria.Categoria;
import com.gg.turnlook.features.sucursal.categoria.ECategoria;
import com.gg.turnlook.features.usuario.Usuario;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class SucursalDisponibilidadGlobalTest {

    @Autowired private SucursalRepository repository;
    @Autowired private EntityManager entityManager;

    @Test
    void incluyeSucursalConServicioDisponibleYEmpleadoActivo() {
        Sucursal operativa = guardarSucursal("Barberia operativa", true);
        guardarServicio(operativa, true);
        agregarEmpleado(operativa, guardarUsuario("operativo@test.com", true));

        Sucursal inactiva = guardarSucursal("Barberia inactiva", false);
        guardarServicio(inactiva, true);
        agregarEmpleado(inactiva, guardarUsuario("inactiva@test.com", true));

        refrescar();

        Set<Integer> visibles = ids(repository.findOperativas(null, null));
        assertTrue(visibles.contains(operativa.getId()));
        assertFalse(visibles.contains(inactiva.getId()));

        assertTrue(ids(repository.findOperativas(ECategoria.BARBERIA, "barberia operativa"))
                .contains(operativa.getId()));
    }

    @Test
    void excluyeSucursalSinServiciosDisponibles() {
        Sucursal sinServicio = guardarSucursal("Sin servicio", true);
        agregarEmpleado(sinServicio, guardarUsuario("sin-servicio@test.com", true));

        Sucursal soloServicioInactivo = guardarSucursal("Solo servicio inactivo", true);
        guardarServicio(soloServicioInactivo, false);
        agregarEmpleado(soloServicioInactivo, guardarUsuario("servicio-inactivo@test.com", true));

        refrescar();

        Set<Integer> visibles = ids(repository.findOperativas(null, null));
        assertFalse(visibles.contains(sinServicio.getId()));
        assertFalse(visibles.contains(soloServicioInactivo.getId()));
    }

    @Test
    void excluyeSucursalSinEmpleadosActivos() {
        Sucursal conEmpleadoInactivo = guardarSucursal("Con empleado inactivo", true);
        guardarServicio(conEmpleadoInactivo, true);
        agregarEmpleado(conEmpleadoInactivo, guardarUsuario("empleado-inactivo@test.com", false));

        Sucursal sinEmpleados = guardarSucursal("Sin empleados", true);
        guardarServicio(sinEmpleados, true);

        refrescar();

        Set<Integer> visibles = ids(repository.findOperativas(null, null));
        assertFalse(visibles.contains(conEmpleadoInactivo.getId()));
        assertFalse(visibles.contains(sinEmpleados.getId()));
    }

    @Test
    void excluyeSucursalQueNoCumpleNingunaCondicion() {
        Sucursal sinCondiciones = guardarSucursal("Sin condiciones", true);
        guardarServicio(sinCondiciones, false);
        agregarEmpleado(sinCondiciones, guardarUsuario("sin-condiciones@test.com", false));

        refrescar();

        assertFalse(ids(repository.findOperativas(null, null)).contains(sinCondiciones.getId()));
    }

    @Test
    void vuelveAAparecerAlRecuperarAmbasCondicionesSinCambiarSuEstado() {
        Sucursal sucursal = guardarSucursal("Se recupera", true);
        Servicio servicio = guardarServicio(sucursal, false);
        Usuario empleado = guardarUsuario("se-recupera@test.com", false);
        agregarEmpleado(sucursal, empleado);
        refrescar();

        assertFalse(ids(repository.findOperativas(null, null)).contains(sucursal.getId()));

        entityManager.find(Servicio.class, servicio.getId()).setActivo(true);
        entityManager.find(Usuario.class, empleado.getId()).setActivo(true);
        entityManager.flush();

        assertTrue(ids(repository.findOperativas(null, null)).contains(sucursal.getId()));
        assertTrue(sucursal.isActivo());
    }

    @Test
    void listadosInternosDelEmpleadorNoAplicanElFiltroDeDisponibilidad() {
        Usuario empleador = guardarUsuario("empleador@test.com", true);
        Usuario empleado = guardarUsuario("empleado-interno@test.com", true);
        Sucursal interna = guardarSucursal("Sucursal interna", true);
        interna.setEmpleador(empleador);
        repository.saveAndFlush(interna);
        agregarEmpleado(interna, empleado);

        refrescar();

        assertTrue(ids(repository.findSucursalesDondeEsta("empleador@test.com"))
                .contains(interna.getId()));
        assertTrue(ids(repository.findSucursalesDondeEsEmpleado("empleado-interno@test.com"))
                .contains(interna.getId()));
        assertFalse(ids(repository.findOperativas(null, null)).contains(interna.getId()));
    }

    private Sucursal guardarSucursal(String nombre, boolean activo) {
        Categoria categoria = new Categoria(ECategoria.BARBERIA);
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

    private Usuario guardarUsuario(String email, boolean activo) {
        Usuario usuario = new Usuario("Nombre", "Apellido", email, "hash");
        usuario.setActivo(activo);
        entityManager.persist(usuario);
        return usuario;
    }

    private Servicio guardarServicio(Sucursal sucursal, boolean activo) {
        Servicio servicio = new Servicio(
                "Servicio de " + sucursal.getNombre(), "Descripcion", 30,
                new BigDecimal("1000"), sucursal);
        servicio.setActivo(activo);
        entityManager.persist(servicio);
        return servicio;
    }

    private void agregarEmpleado(Sucursal sucursal, Usuario empleado) {
        sucursal.getEmpleados().add(empleado);
        repository.saveAndFlush(sucursal);
    }

    private void refrescar() {
        entityManager.flush();
        entityManager.clear();
    }

    private Set<Integer> ids(List<Sucursal> sucursales) {
        return sucursales.stream().map(Sucursal::getId).collect(Collectors.toSet());
    }
}
