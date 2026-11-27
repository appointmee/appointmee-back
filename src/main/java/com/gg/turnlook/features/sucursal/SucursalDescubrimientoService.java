package com.gg.turnlook.features.sucursal;

import com.gg.turnlook.common.exception.BadRequestException;
import com.gg.turnlook.common.util.PaginacionUtil;
import com.gg.turnlook.features.sucursal.categoria.ECategoria;
import com.gg.turnlook.features.sucursal.dto.PaginaSucursalesDTO;
import com.gg.turnlook.features.sucursal.dto.RecomendacionesSucursalesDTO;
import com.gg.turnlook.features.sucursal.dto.SucursalMiniDTO;
import com.gg.turnlook.features.turno.EstadoTurno;
import com.gg.turnlook.features.turno.TurnoRepository;
import com.gg.turnlook.features.usuario.Usuario;
import com.gg.turnlook.features.usuario.UsuarioService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consultas del Home (recomendaciones) y del listado paginado.
 * Reutiliza el ranking 50/50 y el mapeo de SucursalService para no duplicar
 * la logica existente de clasificacion y de DTOs. */
@Service
public class SucursalDescubrimientoService {

    private static final int LIMITE_SECCION = 10;
    private static final int LIMITE_VISTA_PREVIA = 4;
    private static final int TAMANO_PAGINA_DEFECTO = PaginacionUtil.TAMANO_PAGINA;
    private static final int TAMANO_PAGINA_MAXIMO = 50;
    /** Spring resuelve el principal anónimo con este nombre, no con null. */
    private static final String USUARIO_ANONIMO = "anonymousUser";
    private static final Collection<EstadoTurno> ESTADOS_CONTABILIZADOS =
            List.of(EstadoTurno.CONFIRMADO, EstadoTurno.REALIZADO);

    private final SucursalRepository sucRepo;
    private final TurnoRepository turnoRepo;
    private final SucursalVisitaRepository visitaRepo;
    private final UsuarioService usuarioService;
    private final SucursalService sucursalService;

    public SucursalDescubrimientoService(SucursalRepository sucRepo,
                                         TurnoRepository turnoRepo,
                                         SucursalVisitaRepository visitaRepo,
                                         UsuarioService usuarioService,
                                         SucursalService sucursalService) {
        this.sucRepo = sucRepo;
        this.turnoRepo = turnoRepo;
        this.visitaRepo = visitaRepo;
        this.usuarioService = usuarioService;
        this.sucursalService = sucursalService;
    }

    @Transactional(readOnly = true)
    public RecomendacionesSucursalesDTO obtenerRecomendaciones(String email) {
        // El algoritmo actual se ejecuta sobre el pool completo de sucursales
        // operativas; recien despues se corta el top de cada seccion. Asi la
        // normalizacion de popularidad no cambia respecto del listado general.
        List<SucursalMiniDTO> rankeadas = sucursalService.listarSucursales();

        return new RecomendacionesSucursalesDTO(
                rankeadas.stream().limit(LIMITE_SECCION).toList(),
                masDemandadas(),
                nuevas(),
                vistasRecientes(email),
                paraVos(email, rankeadas),
                rankeadas.stream().limit(LIMITE_VISTA_PREVIA).toList());
    }

    private List<SucursalMiniDTO> masDemandadas() {
        List<Object[]> filas = turnoRepo.contarTurnosRecientesPorSucursal(
                ESTADOS_CONTABILIZADOS,
                LocalDateTime.now().minusDays(30),
                PageRequest.of(0, LIMITE_SECCION));

        List<Integer> ids = filas.stream()
                .map(fila -> ((Number) fila[0]).intValue())
                .toList();

        return mapearEnOrdenDeIds(ids);
    }

    private List<SucursalMiniDTO> nuevas() {
        List<Sucursal> sucursales = sucRepo.findOperativasNuevas(
                LocalDate.now().minusMonths(2),
                PageRequest.of(0, LIMITE_SECCION));

        return sucursalService.mapearSucursalesMiniEnOrdenRecibido(sucursales);
    }

    private static boolean esUsuarioAutenticado(String email) {
        return email != null && !email.isBlank() && !USUARIO_ANONIMO.equals(email);
    }

    private List<SucursalMiniDTO> vistasRecientes(String email) {
        if (!esUsuarioAutenticado(email)) {
            return List.of();
        }

        Usuario usuario = usuarioService.listarUsuarioPorEmail(email);
        List<Sucursal> sucursales = visitaRepo.findSucursalesRecientesOperativas(
                usuario.getId(), PageRequest.of(0, LIMITE_SECCION));

        return sucursalService.mapearSucursalesMiniEnOrdenRecibido(sucursales);
    }

    @Transactional
    public void registrarVisita(Integer sucursalId, String email) {
        Sucursal sucursal = sucursalService.listarSucursalPorId(sucursalId);
        Usuario usuario = usuarioService.listarUsuarioPorEmail(email);
        LocalDateTime ahora = LocalDateTime.now();

        visitaRepo.findByUsuarioIdAndSucursalId(usuario.getId(), sucursal.getId())
                .ifPresentOrElse(
                        visita -> {
                            visita.setFechaVisita(ahora);
                            visitaRepo.save(visita);
                        },
                        () -> visitaRepo.save(
                                new SucursalVisita(null, usuario, sucursal, ahora)));
    }

    /** Prioriza las categorias segun la frecuencia ponderada de los turnos del
     * ultimo semestre y, dentro de cada categoria, conserva el ranking actual. */
    private List<SucursalMiniDTO> paraVos(String email,
                                          List<SucursalMiniDTO> rankeadas) {
        if (!esUsuarioAutenticado(email)) {
            return List.of();
        }

        LocalDateTime ahora = LocalDateTime.now();
        List<Object[]> filas = turnoRepo.puntuarCategoriasPorCliente(
                email, ESTADOS_CONTABILIZADOS,
                ahora.minusDays(30), ahora.minusDays(90), ahora.minusMonths(6));

        Set<ECategoria> categoriasPreferidas = filas.stream()
                .map(fila -> fila[0])
                .filter(Objects::nonNull)
                .map(valor -> valor instanceof ECategoria categoria
                        ? categoria
                        : ECategoria.valueOf(valor.toString().toUpperCase(Locale.ROOT)))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (categoriasPreferidas.isEmpty()) {
            return List.of();
        }

        Set<Integer> yaReservadas = new HashSet<>(turnoRepo
                .findSucursalesYaReservadasPorCliente(email, ESTADOS_CONTABILIZADOS));

        // Lista ya ordenada con el algoritmo 50/50 existente. Se filtra por
        // categoria sin recalcular la puntuacion para no duplicar la formula.
        List<SucursalMiniDTO> resultado = new ArrayList<>();

        for (ECategoria categoria : categoriasPreferidas) {
            for (SucursalMiniDTO sucursal : rankeadas) {
                if (resultado.size() == LIMITE_SECCION) {
                    break;
                }
                if (categoria.name().equals(sucursal.getCategoria())
                        && !yaReservadas.contains(sucursal.getId())) {
                    resultado.add(sucursal);
                }
            }
            if (resultado.size() == LIMITE_SECCION) {
                break;
            }
        }

        return resultado;
    }

    /** Listado paginado: el ORDER BY y el OFFSET/LIMIT se resuelven en la base. */
    @Transactional(readOnly = true)
    public PaginaSucursalesDTO listarPaginado(String nombre, String categoria,
                                              String page, String size) {
        ECategoria filtroCategoria = parsearCategoria(categoria);
        String filtroNombre = nombre == null || nombre.isBlank() ? null : nombre.trim();
        int numeroPagina = parsearNumero(page, "page", 0);
        int tamanoSolicitado = parsearNumero(size, "size", TAMANO_PAGINA_DEFECTO);

        validarPaginacion(numeroPagina, tamanoSolicitado);

        // Tamano fijo de esta historia: el cliente puede pedir paginas, no
        // cambiar cuantos registros trae cada una.
        PageRequest pagina = PageRequest.of(numeroPagina, PaginacionUtil.TAMANO_PAGINA);
        List<Integer> ids = sucRepo.findIdsOperativasOrdenadasPorRanking(
                filtroCategoria == null ? null : filtroCategoria.name(),
                filtroNombre,
                pagina);
        long total = sucRepo.contarOperativas(filtroCategoria, filtroNombre);

        PageImpl<SucursalMiniDTO> resultado = new PageImpl<>(
                mapearEnOrdenDeIds(ids), pagina, total);

        return new PaginaSucursalesDTO(
                resultado.getContent(),
                resultado.getTotalElements(),
                resultado.getTotalPages(),
                resultado.getNumber(),
                resultado.getSize());
    }

    /** Listado administrativo: filtros combinables resueltos en la base. */
    @Transactional(readOnly = true)
    public PaginaSucursalesDTO listarPaginadoAdmin(String termino, String page) {
        return listarPaginadoAdmin(termino, null, null, page);
    }

    /** mantiene la busqueda libre por nombre/categoria/id y agrega filtros
     * explicitos por nombre y categoria, que se combinan entre si. */
    @Transactional(readOnly = true)
    public PaginaSucursalesDTO listarPaginadoAdmin(String termino, String nombre,
                                                   String categoria, String page) {
        ECategoria filtroCategoria = parsearCategoria(categoria);
        String filtroNombre = nombre == null || nombre.isBlank() ? null : nombre.trim();
        PageRequest pagina = PageRequest.of(
                PaginacionUtil.pagina(page), PaginacionUtil.TAMANO_PAGINA);

        List<Integer> ids = sucRepo.findIdsOperativasParaAdmin(
                termino, filtroNombre,
                filtroCategoria == null ? null : filtroCategoria.name(), pagina);
        long total = sucRepo.contarOperativasParaAdmin(termino, filtroNombre, filtroCategoria);

        return new PaginaSucursalesDTO(
                mapearEnOrdenDeIds(ids), total,
                (int) Math.ceil((double) total / PaginacionUtil.TAMANO_PAGINA),
                pagina.getPageNumber(), pagina.getPageSize());
    }

    private List<SucursalMiniDTO> mapearEnOrdenDeIds(List<Integer> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        Map<Integer, Sucursal> porId = sucRepo.findByIdsConCategoriaYFoto(ids).stream()
                .collect(Collectors.toMap(Sucursal::getId, Function.identity(), (a, b) -> a));

        List<Sucursal> ordenadas = ids.stream()
                .map(porId::get)
                .filter(Objects::nonNull)
                .toList();

        return sucursalService.mapearSucursalesMiniEnOrdenRecibido(ordenadas);
    }

    private ECategoria parsearCategoria(String categoria) {
        if (categoria == null || categoria.isBlank()) {
            return null;
        }
        try {
            return ECategoria.valueOf(categoria.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Categoría inválida");
        }
    }

    private int parsearNumero(String valor, String nombreParametro, int defecto) {
        if (valor == null || valor.isBlank()) {
            return defecto;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException(
                    "El parámetro " + nombreParametro + " debe ser un número entero");
        }
    }

    private void validarPaginacion(int pagina, int tamano) {
        if (pagina < 0) {
            throw new BadRequestException("El parámetro page no puede ser negativo");
        }
        if (tamano < 1 || tamano > TAMANO_PAGINA_MAXIMO) {
            throw new BadRequestException(
                    "El parámetro size debe estar entre 1 y " + TAMANO_PAGINA_MAXIMO);
        }
    }

}
