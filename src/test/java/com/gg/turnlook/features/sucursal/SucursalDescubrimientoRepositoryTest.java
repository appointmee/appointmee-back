package com.gg.turnlook.features.sucursal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gg.turnlook.features.resenia.Resenia;
import com.gg.turnlook.features.servicio.Servicio;
import com.gg.turnlook.features.sucursal.categoria.Categoria;
import com.gg.turnlook.features.sucursal.categoria.ECategoria;
import com.gg.turnlook.features.turno.EstadoTurno;
import com.gg.turnlook.features.turno.Turno;
import com.gg.turnlook.features.turno.TurnoRepository;
import com.gg.turnlook.features.usuario.Usuario;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

/** Prueba las consultas nuevas: paginacion con ranking en base, novedades,
 * visitas persistentes y agregaciones de turnos sin N+1. */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class SucursalDescubrimientoRepositoryTest {

    private static final Collection<EstadoTurno> ESTADOS_CONTABILIZADOS =
            List.of(EstadoTurno.CONFIRMADO, EstadoTurno.REALIZADO);

    @Autowired private SucursalRepository sucRepo;
    @Autowired private TurnoRepository turnoRepo;
    @Autowired private SucursalVisitaRepository visitaRepo;
    @Autowired private EntityManager entityManager;

    @Test
    void paginacionOrdenaPorRanking50_50YRespetaFiltros() {
        Sucursal conDosResenias = sucursalOperativa("Dos reseñas", ECategoria.SPA,
                LocalDate.now().minusDays(30));
        Sucursal conUnaResenia = sucursalOperativa("Una reseña", ECategoria.SPA,
                LocalDate.now().minusDays(20));
        Sucursal sinResenias = sucursalOperativa("Sin reseñas", ECategoria.BARBERIA,
                LocalDate.now().minusDays(10));

        Resenia conDos1 = guardarResenia(conDosResenias, 5);
        guardarResenia(conDosResenias, 5, conDos1.getTurno().getCliente());
        guardarResenia(conUnaResenia, 5);
        refrescar();

        assertEquals(3, sucRepo.contarOperativas(null, null));
        assertEquals(List.of(conDosResenias.getId()),
                sucRepo.findIdsOperativasOrdenadasPorRanking(
                        null, null, PageRequest.of(0, 1)));
        assertEquals(List.of(conUnaResenia.getId()),
                sucRepo.findIdsOperativasOrdenadasPorRanking(
                        null, null, PageRequest.of(1, 1)));

        assertEquals(2, sucRepo.contarOperativas(ECategoria.SPA, null));
        assertEquals(List.of(conDosResenias.getId(), conUnaResenia.getId()),
                sucRepo.findIdsOperativasOrdenadasPorRanking(
                        ECategoria.SPA.name(), null, PageRequest.of(0, 10)));

        assertEquals(1, sucRepo.contarOperativas(null, "sin reseñas"));
        assertEquals(List.of(sinResenias.getId()),
                sucRepo.findIdsOperativasOrdenadasPorRanking(
                        null, "sin reseñas", PageRequest.of(0, 10)));

        // El buscador historico tambien acepta el id exacto como termino.
        String idComoTexto = String.valueOf(sinResenias.getId());
        assertEquals(1, sucRepo.contarOperativas(null, idComoTexto));
        assertEquals(List.of(sinResenias.getId()),
                sucRepo.findIdsOperativasOrdenadasPorRanking(
                        null, idComoTexto, PageRequest.of(0, 10)));
    }

    @Test
    void nuevasIncluyeSoloOperativasCreadasEnLosUltimosDosMeses() {
        Sucursal reciente = sucursalOperativa("Reciente", ECategoria.SPA,
                LocalDate.now().minusDays(5));
        Sucursal masReciente = sucursalOperativa("Más reciente", ECategoria.SPA,
                LocalDate.now().minusDays(1));
        Sucursal antigua = sucursalOperativa("Antigua", ECategoria.SPA,
                LocalDate.now().minusMonths(3));

        Sucursal inactiva = sucursalOperativa("Inactiva", ECategoria.SPA,
                LocalDate.now().minusDays(2));
        inactiva.setActivo(false);
        sucRepo.saveAndFlush(inactiva);

        refrescar();

        assertEquals(List.of("Más reciente", "Reciente"),
                nombres(sucRepo.findOperativasNuevas(
                        LocalDate.now().minusMonths(2), PageRequest.of(0, 10))));
    }

    @Test
    void visitasRecientesExcluyeSucursalesNoOperativasYNoRepiteSucursal() {
        Usuario usuario = guardarUsuario("visitante@test.com", true);
        Sucursal primera = sucursalOperativa("Primera visitada", ECategoria.SPA,
                LocalDate.now().minusDays(10));
        Sucursal segunda = sucursalOperativa("Segunda visitada", ECategoria.SPA,
                LocalDate.now().minusDays(10));
        Sucursal inactiva = sucursalOperativa("Visitada inactiva", ECategoria.SPA,
                LocalDate.now().minusDays(10));
        inactiva.setActivo(false);
        sucRepo.saveAndFlush(inactiva);
        Sucursal sinServicio = sucursal("Sin servicio", ECategoria.SPA,
                LocalDate.now().minusDays(10));
        sucRepo.saveAndFlush(sinServicio);

        guardarVisita(usuario, segunda, LocalDateTime.now().minusDays(1));
        guardarVisita(usuario, primera, LocalDateTime.now().minusDays(2));
        guardarVisita(usuario, inactiva, LocalDateTime.now().minusMinutes(5));
        guardarVisita(usuario, sinServicio, LocalDateTime.now().minusMinutes(1));
        refrescar();

        assertEquals(List.of(segunda.getId(), primera.getId()),
                ids(visitaRepo.findSucursalesRecientesOperativas(
                        usuario.getId(), PageRequest.of(0, 10))));
    }

    @Test
    void demandaRecienteCuentaSoloTurnosConfirmadosYRealizadosDeUltimos30Dias() {
        LocalDateTime ahora = LocalDateTime.now();
        Sucursal masDemandada = sucursalOperativa("Más demandada", ECategoria.SPA,
                LocalDate.now().minusDays(10));
        Sucursal segunda = sucursalOperativa("Segunda", ECategoria.SPA,
                LocalDate.now().minusDays(10));
        Servicio servicioMasDemandada = servicioActivo(masDemandada);
        Servicio servicioSegunda = servicioActivo(segunda);
        Usuario cliente = guardarUsuario("cliente-demanda@test.com", true);

        guardarTurno(servicioMasDemandada, cliente, EstadoTurno.CONFIRMADO, ahora.minusDays(1));
        guardarTurno(servicioMasDemandada, cliente, EstadoTurno.REALIZADO, ahora.minusDays(2));
        guardarTurno(servicioSegunda, cliente, EstadoTurno.REALIZADO, ahora.minusDays(3));
        guardarTurno(servicioSegunda, cliente, EstadoTurno.PENDIENTE, ahora.minusDays(4));
        guardarTurno(servicioSegunda, cliente, EstadoTurno.CANCELADO, ahora.minusDays(5));
        guardarTurno(servicioSegunda, cliente, EstadoTurno.PAGO_RECHAZADO, ahora.minusDays(6));
        guardarTurno(servicioSegunda, cliente, EstadoTurno.CONFIRMADO, ahora.minusDays(45));
        refrescar();

        Map<Integer, Long> conteos = turnoRepo.contarTurnosRecientesPorSucursal(
                ESTADOS_CONTABILIZADOS, ahora.minusDays(30), PageRequest.of(0, 10)).stream()
                .collect(Collectors.toMap(
                        fila -> ((Number) fila[0]).intValue(),
                        fila -> ((Number) fila[1]).longValue()));

        assertEquals(2, conteos.size());
        assertEquals(2L, conteos.get(masDemandada.getId()));
        assertEquals(1L, conteos.get(segunda.getId()));
    }

    @Test
    void recomendacionPonderaCategoriasSegunAntiguedadDelTurno() {
        LocalDateTime ahora = LocalDateTime.now();
        Sucursal spa = sucursalOperativa("Spa favorito", ECategoria.SPA,
                LocalDate.now().minusDays(20));
        Sucursal barberia = sucursalOperativa("Barbería frecuente", ECategoria.BARBERIA,
                LocalDate.now().minusDays(20));
        Sucursal peluqueria = sucursalOperativa("Peluquería antigua", ECategoria.PELUQUERIA,
                LocalDate.now().minusDays(20));
        Usuario cliente = guardarUsuario("cliente-recomendacion@test.com", true);

        guardarTurno(servicioActivo(spa), cliente,
                EstadoTurno.CONFIRMADO, ahora.minusDays(10));
        guardarTurno(servicioActivo(barberia), cliente,
                EstadoTurno.REALIZADO, ahora.minusDays(60));
        guardarTurno(servicioActivo(peluqueria), cliente,
                EstadoTurno.CONFIRMADO, ahora.minusDays(120));
        guardarTurno(servicioActivo(spa), cliente,
                EstadoTurno.CONFIRMADO, ahora.minusDays(200));
        refrescar();

        List<Object[]> filas = turnoRepo.puntuarCategoriasPorCliente(
                cliente.getEmail(), ESTADOS_CONTABILIZADOS,
                ahora.minusDays(30), ahora.minusDays(90), ahora.minusMonths(6));

        assertEquals(3, filas.size());
        assertEquals("SPA", filas.get(0)[0].toString());
        assertEquals(1.0, ((Number) filas.get(0)[1]).doubleValue(), 0.0001);
        assertEquals("BARBERIA", filas.get(1)[0].toString());
        assertEquals(0.7, ((Number) filas.get(1)[1]).doubleValue(), 0.0001);
        assertEquals("PELUQUERIA", filas.get(2)[0].toString());
        assertEquals(0.4, ((Number) filas.get(2)[1]).doubleValue(), 0.0001);
    }

    @Test
    void sucursalesYaReservadasIgnoraTurnosSinConfirmar() {
        LocalDateTime ahora = LocalDateTime.now();
        Sucursal reservada = sucursalOperativa("Ya reservada", ECategoria.SPA,
                LocalDate.now().minusDays(20));
        Sucursal pendiente = sucursalOperativa("Solo pendiente", ECategoria.SPA,
                LocalDate.now().minusDays(20));
        Usuario cliente = guardarUsuario("cliente-yareservada@test.com", true);

        guardarTurno(servicioActivo(reservada), cliente,
                EstadoTurno.CONFIRMADO, ahora.minusDays(1));
        guardarTurno(servicioActivo(pendiente), cliente,
                EstadoTurno.PENDIENTE, ahora.minusDays(1));
        refrescar();

        assertEquals(List.of(reservada.getId()),
                turnoRepo.findSucursalesYaReservadasPorCliente(
                        cliente.getEmail(), ESTADOS_CONTABILIZADOS));
    }

    private Sucursal sucursalOperativa(String nombre, ECategoria categoria,
                                       LocalDate fechaCreacion) {
        Sucursal sucursal = sucursal(nombre, categoria, fechaCreacion);
        guardarServicio(sucursal, true);
        agregarEmpleado(sucursal, guardarUsuario(
                "empleado-" + nombre.hashCode() + "@test.com", true));
        return sucursal;
    }

    private Sucursal sucursal(String nombre, ECategoria categoria,
                              LocalDate fechaCreacion) {
        Categoria categoriaEntidad = new Categoria(categoria);
        entityManager.persist(categoriaEntidad);

        Sucursal sucursal = new Sucursal();
        sucursal.setNombre(nombre);
        sucursal.setDireccion("Dirección " + nombre);
        sucursal.setDescripcion("Sucursal de prueba");
        sucursal.setFechaCreacion(fechaCreacion);
        sucursal.setHoraApertura(LocalTime.of(9, 0));
        sucursal.setHoraCierre(LocalTime.of(18, 0));
        sucursal.setActivo(true);
        sucursal.setCategoria(categoriaEntidad);
        Sucursal guardada = sucRepo.saveAndFlush(sucursal);

        // @PrePersist de Sucursal siempre guarda la fecha actual. Las pruebas de
        // novedades necesitan fechas anteriores, asi que se ajustan en la base.
        entityManager.createNativeQuery(
                        "UPDATE sucursales SET fecha_creacion = :fecha WHERE id = :id")
                .setParameter("fecha", fechaCreacion)
                .setParameter("id", guardada.getId())
                .executeUpdate();
        entityManager.refresh(guardada);
        return guardada;
    }

    private Servicio servicioActivo(Sucursal sucursal) {
        Servicio servicio = new Servicio(
                "Servicio de " + sucursal.getNombre(), "Descripción", 30,
                new BigDecimal("1000"), sucursal);
        servicio.setActivo(true);
        entityManager.persist(servicio);
        entityManager.flush();
        return servicio;
    }

    private void guardarServicio(Sucursal sucursal, boolean activo) {
        Servicio servicio = servicioActivo(sucursal);
        servicio.setActivo(activo);
        entityManager.flush();
    }

    private Usuario guardarUsuario(String email, boolean activo) {
        Usuario usuario = new Usuario("Nombre", "Apellido", email, "hash");
        usuario.setActivo(activo);
        entityManager.persist(usuario);
        entityManager.flush();
        return usuario;
    }

    private void agregarEmpleado(Sucursal sucursal, Usuario empleado) {
        sucursal.getEmpleados().add(empleado);
        sucRepo.saveAndFlush(sucursal);
    }

    private Turno guardarTurno(Servicio servicio, Usuario cliente,
                               EstadoTurno estado, LocalDateTime fechaHora) {
        Turno turno = new Turno(fechaHora, cliente, null, servicio);
        turno.setFechaReserva(fechaHora.minusDays(1));
        turno.setEstado(estado);
        entityManager.persist(turno);
        entityManager.flush();
        return turno;
    }

    private Resenia guardarResenia(Sucursal sucursal, Integer puntuacion) {
        return guardarResenia(sucursal, puntuacion, null);
    }

    private Resenia guardarResenia(Sucursal sucursal, Integer puntuacion,
                                   Usuario clienteExistente) {
        Usuario cliente = clienteExistente == null
                ? guardarUsuario("resenia-" + sucursal.getId() + "-" + puntuacion
                        + "-" + System.nanoTime() + "@test.com", true)
                : clienteExistente;
        Turno turno = guardarTurno(servicioActivo(sucursal), cliente,
                EstadoTurno.REALIZADO, LocalDateTime.now().minusDays(3));
        Resenia resenia = new Resenia(puntuacion, "Comentario", turno);
        entityManager.persist(resenia);
        entityManager.flush();
        return resenia;
    }

    private void guardarVisita(Usuario usuario, Sucursal sucursal, LocalDateTime fecha) {
        visitaRepo.saveAndFlush(new SucursalVisita(null, usuario, sucursal, fecha));
    }

    private void refrescar() {
        entityManager.flush();
        entityManager.clear();
    }

    private List<Integer> ids(List<Sucursal> sucursales) {
        return sucursales.stream().map(Sucursal::getId).toList();
    }

    private List<String> nombres(List<Sucursal> sucursales) {
        return sucursales.stream().map(Sucursal::getNombre).toList();
    }

}
