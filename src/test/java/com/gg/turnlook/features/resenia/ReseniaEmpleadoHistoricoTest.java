package com.gg.turnlook.features.resenia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gg.turnlook.features.resenia.dto.ReseniaCrearDTO;
import com.gg.turnlook.features.resenia.dto.ReseniaResponseDTO;
import com.gg.turnlook.features.servicio.Servicio;
import com.gg.turnlook.features.sucursal.Sucursal;
import com.gg.turnlook.features.sucursal.SucursalRepository;
import com.gg.turnlook.features.sucursal.categoria.Categoria;
import com.gg.turnlook.features.sucursal.categoria.ECategoria;
import com.gg.turnlook.features.turno.EstadoTurno;
import com.gg.turnlook.features.turno.Turno;
import com.gg.turnlook.features.turno.TurnoRepository;
import com.gg.turnlook.features.usuario.Usuario;
import com.gg.turnlook.features.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

/** SCRUM-368: snapshot historico y vigencia del empleado en las resenias. */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class ReseniaEmpleadoHistoricoTest {

    @Autowired private EntityManager entityManager;
    @Autowired private ReseniaRepository reseniaRepo;
    @Autowired private SucursalRepository sucursalRepo;
    @Autowired private UsuarioRepository usuarioRepo;
    @Autowired private TurnoRepository turnoRepo;

    @Test
    void sinIncluirEmpleadoNoSeGuardaNiSeMuestraInformacionDelEmpleado() {
        Fixture fixture = crearFixture(false);

        Resenia guardada = reseniaRepo.findById(fixture.reseniaId()).orElseThrow();
        assertNull(guardada.getEmpleadoId());
        assertNull(guardada.getEmpleadoNombre());
        assertNull(guardada.getEmpleadoApellido());

        ReseniaResponseDTO dto = unicaResenia(fixture.sucursalId());
        assertNull(dto.getEmpleadoNombre());
        assertNull(dto.getEmpleadoApellido());
        assertFalse(dto.getEmpleadoPerteneceASucursal());
    }

    @Test
    void alIncluirEmpleadoSeCopianSusDatosDelTurno() {
        Fixture fixture = crearFixture(true);

        Resenia guardada = reseniaRepo.findById(fixture.reseniaId()).orElseThrow();
        assertEquals(fixture.empleadoId(), guardada.getEmpleadoId());
        assertEquals("Ana", guardada.getEmpleadoNombre());
        assertEquals("Pérez", guardada.getEmpleadoApellido());

        ReseniaResponseDTO dto = unicaResenia(fixture.sucursalId());
        assertEquals("Ana", dto.getEmpleadoNombre());
        assertEquals("Pérez", dto.getEmpleadoApellido());
        assertTrue(dto.getEmpleadoPerteneceASucursal());
        assertEquals(5, dto.getPuntuacion());
        assertEquals("Comentario de prueba", dto.getComentario());
    }

    @Test
    void desvincularEmpleadoConservaElNombreYMarcaQueYaNoPertenece() {
        Fixture fixture = crearFixture(true);

        Sucursal sucursal = sucursalRepo.findById(fixture.sucursalId()).orElseThrow();
        Usuario empleado = usuarioRepo.findById(fixture.empleadoId()).orElseThrow();
        sucursal.getEmpleados().remove(empleado);
        sucursalRepo.saveAndFlush(sucursal);
        refrescar();

        ReseniaResponseDTO dto = unicaResenia(fixture.sucursalId());
        assertEquals("Ana Pérez", dto.getEmpleadoNombre() + " " + dto.getEmpleadoApellido());
        assertFalse(dto.getEmpleadoPerteneceASucursal());
        assertEquals(5, dto.getPuntuacion());
        assertEquals("Comentario de prueba", dto.getComentario());
    }

    @Test
    void bajaDeLaCuentaConservaElNombreYMarcaQueYaNoPertenece() {
        Fixture fixture = crearFixture(true);

        Usuario empleado = usuarioRepo.findById(fixture.empleadoId()).orElseThrow();
        empleado.setActivo(false);
        usuarioRepo.saveAndFlush(empleado);
        refrescar();

        ReseniaResponseDTO dto = unicaResenia(fixture.sucursalId());
        assertEquals("Ana Pérez", dto.getEmpleadoNombre() + " " + dto.getEmpleadoApellido());
        assertFalse(dto.getEmpleadoPerteneceASucursal());
    }

    @Test
    void aunqueDesaparezcaLaRelacionConElEmpleadoSeConservaElHistorico() {
        Fixture fixture = crearFixture(true);

        Turno turno = turnoRepo.findById(fixture.turnoId()).orElseThrow();
        turno.setEmpleado(null);
        turnoRepo.saveAndFlush(turno);
        refrescar();

        ReseniaResponseDTO dto = unicaResenia(fixture.sucursalId());
        assertEquals("Ana Pérez", dto.getEmpleadoNombre() + " " + dto.getEmpleadoApellido());
        assertFalse(dto.getEmpleadoPerteneceASucursal());
        assertEquals(5, dto.getPuntuacion());
    }

    private ReseniaResponseDTO unicaResenia(Integer sucursalId) {
        var page = reseniaRepo.findPageReseniasPorSucursal(sucursalId, PageRequest.of(0, 10));
        assertEquals(1, page.getTotalElements());
        return page.getContent().get(0);
    }

    private Fixture crearFixture(boolean incluirEmpleado) {
        Sucursal sucursal = sucursal();
        Usuario cliente = usuario("Cliente", "Reseñador");
        Usuario empleado = usuario("Ana", "Pérez");
        sucursal.getEmpleados().add(empleado);
        sucursalRepo.saveAndFlush(sucursal);

        Servicio servicio = servicio(sucursal);
        LocalDateTime fechaHora = LocalDateTime.now().minusDays(2);
        Turno turno = new Turno(fechaHora, cliente, empleado, servicio);
        turno.setFechaReserva(fechaHora.minusDays(1));
        turno.setEstado(EstadoTurno.REALIZADO);
        entityManager.persist(turno);
        entityManager.flush();

        ReseniaCrearDTO dto = new ReseniaCrearDTO();
        dto.setPuntuacion(5);
        dto.setComentario("Comentario de prueba");
        dto.setIncluirEmpleado(incluirEmpleado);
        new ReseniaService(reseniaRepo).guardarResenia(dto, turno);
        entityManager.flush();
        refrescar();

        Resenia resenia = reseniaRepo.findAll().get(0);
        return new Fixture(sucursal.getId(), resenia.getId(), turno.getId(), empleado.getId());
    }

    private Usuario usuario(String nombre, String apellido) {
        Usuario usuario = new Usuario(
                nombre, apellido, UUID.randomUUID() + "@example.com", "hash");
        usuario.setActivo(true);
        entityManager.persist(usuario);
        entityManager.flush();
        return usuario;
    }

    private Sucursal sucursal() {
        Categoria categoria = new Categoria(ECategoria.SPA);
        entityManager.persist(categoria);
        Sucursal sucursal = new Sucursal();
        sucursal.setNombre("Sucursal histórico");
        sucursal.setDireccion("Dirección " + UUID.randomUUID());
        sucursal.setDescripcion("Sucursal de prueba");
        sucursal.setHoraApertura(LocalTime.of(9, 0));
        sucursal.setHoraCierre(LocalTime.of(18, 0));
        sucursal.setActivo(true);
        sucursal.setCategoria(categoria);
        return sucursalRepo.saveAndFlush(sucursal);
    }

    private Servicio servicio(Sucursal sucursal) {
        Servicio servicio = new Servicio(
                "Servicio", "Descripción", 30, new BigDecimal("1000"), sucursal);
        servicio.setActivo(true);
        entityManager.persist(servicio);
        entityManager.flush();
        return servicio;
    }

    private void refrescar() {
        entityManager.flush();
        entityManager.clear();
    }

    private record Fixture(Integer sucursalId, Integer reseniaId,
                           Integer turnoId, Integer empleadoId) {
    }
}
