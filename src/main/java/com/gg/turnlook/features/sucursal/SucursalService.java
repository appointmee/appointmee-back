package com.gg.turnlook.features.sucursal;

import com.gg.turnlook.common.exception.BadRequestException;
import com.gg.turnlook.common.exception.ConflictException;
import com.gg.turnlook.common.exception.ForbiddenException;
import com.gg.turnlook.common.exception.NotFoundException;
import com.gg.turnlook.common.util.DiasSemanaUtil;
import com.gg.turnlook.features.imagen.Imagen;
import com.gg.turnlook.features.imagen.ImagenRepository;
import com.gg.turnlook.features.imagen.ImagenService;
import com.gg.turnlook.features.imagen.dto.ImagenDTO;
import com.gg.turnlook.features.imagen.dto.ImagenResponseDTO;
import com.gg.turnlook.features.resenia.ReseniaService;
import com.gg.turnlook.features.resenia.ReseniaService.ResumenPuntuacion;
import com.gg.turnlook.features.solicitud.empleado.GestionEmpleadoService;
import com.gg.turnlook.features.sucursal.categoria.Categoria;
import com.gg.turnlook.features.sucursal.categoria.CategoriaRepository;
import com.gg.turnlook.features.sucursal.categoria.ECategoria;
import com.gg.turnlook.features.sucursal.dto.SucursalCrearDTO;
import com.gg.turnlook.features.sucursal.dto.SucursalMiniDTO;
import com.gg.turnlook.features.sucursal.dto.SucursalModificarDTO;
import com.gg.turnlook.features.sucursal.dto.SucursalResponseDTO;
import com.gg.turnlook.features.usuario.FotoPerfilUsuario;
import com.gg.turnlook.features.usuario.Usuario;
import com.gg.turnlook.features.usuario.UsuarioService;
import com.gg.turnlook.features.usuario.dto.EmpleadoResponseDTO;
import com.gg.turnlook.features.usuario.dto.UsuarioEmailDTO;
import com.gg.turnlook.features.usuario.dto.UsuarioMiniDTO;
import com.gg.turnlook.features.usuario.dto.UsuarioResponseDTO;
import com.gg.turnlook.features.usuario.rol.ERol;
import com.gg.turnlook.features.usuario.rol.Rol;
import com.gg.turnlook.features.usuario.rol.RolService;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;





@Service
public class SucursalService {


    private final SucursalRepository sucRepo;
    private final CategoriaRepository catRepo;
    private final ImagenRepository imagenRepo;
    private final UsuarioService usuarioService;
    private final ImagenService imagenService;
    private final ReseniaService reseniaService;
    private final GestionEmpleadoService gestionEmpleadoService;
    private final RolService rolService;



    public SucursalService(SucursalRepository sucRepo, CategoriaRepository catRepo, ImagenRepository imagenRepo, UsuarioService usuarioService, ImagenService imagenService, ReseniaService reseniaService, GestionEmpleadoService gestionEmpleadoService, RolService rolService) {
        this.sucRepo = sucRepo;
        this.catRepo = catRepo;
        this.imagenRepo = imagenRepo;
        this.usuarioService = usuarioService;
        this.imagenService = imagenService;
        this.reseniaService = reseniaService;
        this.gestionEmpleadoService = gestionEmpleadoService;
        this.rolService = rolService;
    }


    /// METODOS


    /** Devuelve las categorías con su id para que el frontend pueda enviar categoriaId. */
    public java.util.List<com.gg.turnlook.features.sucursal.categoria.Categoria> listarCategoriasConId() {
        return catRepo.findAll();
    }

    public boolean enSucursal(String userEmail, Sucursal sucursal) {

        // Consulta anterior a la optimización. Reemplazada por contarUsuarioEnSucursal.
        // return sucursal.getEmpleador().getEmail().equals(userEmail) ||
        //         sucursal.getEmpleados().stream()
        //                 .anyMatch(u -> u.getEmail().equals(userEmail));
        return sucRepo.contarUsuarioEnSucursal(sucursal.getId(), userEmail) > 0;
    }


    public void crearSucursal(SucursalCrearDTO sucursal, String empleadorEmail) {

        if (!sucursal.getHoraCierre().isAfter(sucursal.getHoraApertura())) {
            throw new BadRequestException("El horario de cierre tiene que ser posterior al de apertura");
        }

        if (sucRepo.existsByDireccionIgnoreCase(sucursal.getDireccion())) {
            throw new ConflictException("La direccion ingresada ya existe");
        }

        String googlePlaceId = normalizarPlaceId(sucursal.getGooglePlaceId());
        validarPlaceIdUnico(googlePlaceId, null);

        Categoria cat = catRepo.findById(sucursal.getCategoriaId()).
                orElseThrow(() -> new NotFoundException("No se encontró la categoria"));

        Usuario empleador = usuarioService.listarUsuarioPorEmail(empleadorEmail);

        Sucursal suc = new Sucursal(sucursal.getNombre(), sucursal.getDireccion(),
                sucursal.getTelefono(), sucursal.getDescripcion(),
                sucursal.getHoraApertura(), sucursal.getHoraCierre(),
                cat, empleador);

        suc.setDiasAperturaPorNombre(DiasSemanaUtil.aDias(sucursal.getDiasApertura()));

        suc.setLatitud(sucursal.getLatitud());
        suc.setLongitud(sucursal.getLongitud());
        suc.setGooglePlaceId(googlePlaceId);

        suc.setFotoPerfil(imagenService.crearFotoPerfil(sucursal.getFotoUrl(), sucursal.getFotoPublicId()));

        guardarConUnicidadDePlaceId(suc);
    }


    public void modificarSucursal(SucursalModificarDTO sucursal,
                                  Integer sucursalId, String empleadorEmail) {

        Sucursal suc = listarSucursalPorId(sucursalId);

        if (!suc.getEmpleador().getEmail().equals(empleadorEmail)) {
            throw new ForbiddenException("No tenes permisos");
        }

        boolean latitudEnviada = sucursal.getLatitud() != null;
        boolean longitudEnviada = sucursal.getLongitud() != null;
        boolean placeIdEnviado = sucursal.getGooglePlaceId() != null;
        boolean ubicacionCompleta = latitudEnviada && longitudEnviada && placeIdEnviado
                && !sucursal.getGooglePlaceId().isBlank();

        if ((latitudEnviada || longitudEnviada || placeIdEnviado) && !ubicacionCompleta) {
            throw new BadRequestException("La ubicacion debe enviarse completa: latitud, longitud y Google Place ID");
        }

        boolean direccionCambia = sucursal.getDireccion() != null
                && !sucursal.getDireccion().equalsIgnoreCase(suc.getDireccion());

        if (direccionCambia && suc.tieneUbicacion() && !ubicacionCompleta) {
            throw new BadRequestException("Al cambiar la direccion de una sucursal con ubicacion "
                    + "debe enviar latitud, longitud y Google Place ID");
        }

        if (sucursal.getCategoriaId() != null) {
            Categoria cat = catRepo.findById(sucursal.getCategoriaId()).
                    orElseThrow(() -> new NotFoundException("No se encontró la categoria"));
            suc.setCategoria(cat);
        }

        if (sucursal.getNombre() != null) suc.setNombre(sucursal.getNombre());

        if (sucursal.getDireccion() != null &&
                !sucursal.getDireccion().equalsIgnoreCase(suc.getDireccion())) {
            if (sucRepo.existsByDireccionIgnoreCase(sucursal.getDireccion())) {
                throw new ConflictException("La direccion ingresada ya existe");
            }
            suc.setDireccion(sucursal.getDireccion());
        }

        if (sucursal.getTelefono() != null) suc.setTelefono(sucursal.getTelefono());

        if (sucursal.getDescripcion() != null) suc.setDescripcion(sucursal.getDescripcion());

        if (sucursal.getHoraApertura() != null) suc.setHoraApertura(sucursal.getHoraApertura());

        if (sucursal.getHoraCierre() != null) suc.setHoraCierre(sucursal.getHoraCierre());

        if (sucursal.getDiasApertura() != null) {
            suc.setDiasAperturaPorNombre(DiasSemanaUtil.aDias(sucursal.getDiasApertura()));
        }

        if (ubicacionCompleta) {
            String googlePlaceId = normalizarPlaceId(sucursal.getGooglePlaceId());
            if (!googlePlaceId.equals(suc.getGooglePlaceId())) {
                validarPlaceIdUnico(googlePlaceId, suc.getId());
            }
            suc.setLatitud(sucursal.getLatitud());
            suc.setLongitud(sucursal.getLongitud());
            suc.setGooglePlaceId(googlePlaceId);
        }

        if (!suc.getHoraCierre().isAfter(suc.getHoraApertura())) {
            throw new BadRequestException("El horario de cierre tiene que ser posterior al de apertura");
        }

        // ver si valida bien como ultimo dsp
        if (sucursal.getFotoUrl() != null && !sucursal.getFotoUrl().isBlank()) {
            imagenService.cambiarFotoPerfilSucursal(suc, sucursal.getFotoUrl(), sucursal.getFotoPublicId());
        }

        guardarConUnicidadDePlaceId(suc);
    }


    /// El Place ID viaja tal cual lo devuelve Google; sacar espacios evita que
    /// el mismo lugar se guarde dos veces con distinta cantidad de espacios.

    private static String normalizarPlaceId(String googlePlaceId) {
        return googlePlaceId == null ? null : googlePlaceId.trim();
    }


    private void validarPlaceIdUnico(String googlePlaceId, Integer idExcluido) {

        boolean existe = idExcluido == null
                ? sucRepo.existsByActivoTrueAndGooglePlaceId(googlePlaceId)
                : sucRepo.existsByActivoTrueAndGooglePlaceIdAndIdNot(googlePlaceId, idExcluido);

        if (existe) {
            throw new ConflictException("El Google Place ID ya esta asociado a otra sucursal");
        }
    }


    /// El indice parcial de PostgreSQL es la ultima linea de defensa; si una
    /// insercion/concurrencia lo viola, lo traducimos al mismo 409 que el
    /// chequeo previo en vez de dejar que caiga en el error generico.

    private void guardarConUnicidadDePlaceId(Sucursal sucursal) {
        try {
            sucRepo.save(sucursal);
        } catch (DataIntegrityViolationException e) {
            Throwable causa = e.getMostSpecificCause();
            String mensaje = causa.getMessage() == null
                    ? "" : causa.getMessage().toLowerCase(java.util.Locale.ROOT);
            if (mensaje.contains("google_place_id") || mensaje.contains("ux_sucursales_google_place_id")) {
                throw new ConflictException("El Google Place ID ya esta asociado a otra sucursal");
            }
            throw e;
        }
    }


    public List<ImagenResponseDTO> listarImagenesPorSucursal(Integer sucursalId,
                                                             String empleadorEmail) {

        Sucursal sucursal = listarSucursalPorId(sucursalId);

        if (!esEmpleadorAca(sucursal, empleadorEmail)) {
            throw new ForbiddenException("No tenes permisos");
        }

        return imagenService.listarImagenesPorSucursal(sucursal);
    }


    public void agregarImagenes(ImagenDTO imagenes, Integer sucursalId,
                                String empleadorEmail) {

        Sucursal sucursal = listarSucursalPorId(sucursalId);

        if (!esEmpleadorAca(sucursal, empleadorEmail)) {
            throw new ForbiddenException("No tenes permisos");
        }

        long totalImagenes = imagenService.cuantasImagenesPorSucursal(sucursal);

        if (imagenes.getUrls().size() + totalImagenes > 5) {
            throw new ConflictException("Una sucursal no puede tener mas de 5 imagenes");
        }

        for (String imagen : imagenes.getUrls()) {
            imagenService.crearImagenSucursal(sucursal, imagen);
        }

    }

    public ImagenResponseDTO subirImagen(Integer sucursalId, MultipartFile archivo,
                                         String empleadorEmail) {
        Sucursal sucursal = listarSucursalPorId(sucursalId);
        if (!esEmpleadorAca(sucursal, empleadorEmail)) {
            throw new ForbiddenException("No tenes permisos");
        }
        if (imagenService.cuantasImagenesPorSucursal(sucursal) >= 5) {
            throw new ConflictException("Una sucursal no puede tener mas de 5 imagenes");
        }
        return imagenService.subirImagenSucursal(sucursal, archivo);
    }


    public void eliminarImagen(Integer imagenId, Integer sucursalId,
                               String empleadorEmail) {

        Sucursal sucursal = listarSucursalPorId(sucursalId);

        if (!esEmpleadorAca(sucursal, empleadorEmail)) {
            throw new ForbiddenException("No tenes permisos");
        }

        Imagen img = imagenService.listarImagenPorId(imagenId);

        if (!Objects.equals(img.getSucursal().getId(), sucursal.getId())) {
            throw new ForbiddenException("La imagen no le corresponde a esta sucursal");
        }

        imagenService.eliminarImagen(img);
    }


    public void eliminarSucursal(Integer sucursalId) {

        Sucursal sucursal = listarSucursalPorId(sucursalId);

        inactivarSucursal(sucursal);
    }


    public void borrarSucursalPropia(Integer sucursalId, String empleadorEmail) {

        Sucursal sucursal = listarSucursalPorId(sucursalId);

        if (!esEmpleadorAca(sucursal, empleadorEmail)) {
            throw new ForbiddenException("No tenes permisos");
        }

        inactivarSucursal(sucursal);
    }


    /// La baja no borra los datos de la sucursal: solo la inactiva y libera su
    /// Google Place ID para que otra sucursal activa pueda usarlo.

    private void inactivarSucursal(Sucursal sucursal) {
        sucursal.setActivo(false);
        sucursal.setGooglePlaceId(null);
        sucRepo.save(sucursal);
    }


    public List<SucursalMiniDTO> listarSucursales() {

        return mapearSucursalesMini(sucRepo.findOperativas(null, null));
    }


    /// Mapea una lista de sucursales a DTOs trayendo las puntuaciones de todas
    /// en una sola query. La categoria y la foto ya vienen cargadas por el
    /// @EntityGraph del repository, asi que no hay queries por sucursal.

    public List<SucursalMiniDTO> mapearSucursalesMini(List<Sucursal> sucursales) {

        return ordenarSucursalesPorPuntuacionYPopularidad(
                mapearSucursalesMiniEnOrdenRecibido(sucursales));
    }

    /** Mapea trayendo las puntuaciones en una sola query, pero conserva el
     * orden recibido. Lo usan las secciones cuyo orden no es el ranking 50/50. */
    public List<SucursalMiniDTO> mapearSucursalesMiniEnOrdenRecibido(
            List<Sucursal> sucursales) {

        Map<Integer, ResumenPuntuacion> puntuaciones =
                reseniaService.getPuntuacionesSucursales(
                        sucursales.stream().map(Sucursal::getId).toList());

        return new java.util.ArrayList<>(sucursales.stream()
                .map(suc -> new SucursalMiniDTO(
                        suc.getId(), suc.getNombre(),
                        suc.getCategoria().getCategoria().name(),
                        suc.getFotoPerfil() == null ? null : suc.getFotoPerfil().getFotoValida(),
                        puntuacionDe(puntuaciones.get(suc.getId())),
                        cantidadDe(puntuaciones.get(suc.getId())),
                        DiasSemanaUtil.aNombres(suc.getDiasApertura()),
                        suc.getLatitud(), suc.getLongitud())
                )
                .toList());
    }


    private static Double puntuacionDe(ResumenPuntuacion resumen) {

        return resumen == null ? null : resumen.promedioConUnDecimal();
    }


    private static Long cantidadDe(ResumenPuntuacion resumen) {

        return resumen == null ? 0L : resumen.cantidad();
    }


    /// Ordena sucursales combinando puntuacion promedio y popularidad (cantidad de resenias)
    /// con ponderacion 50/50. Ambas metricas se normalizan a [0, 1] dentro del listado:
    /// - Puntuacion: (promedio - 1) / 4 cuando hay resenias; 0 si no hay (promedio en escala 1-5).
    /// - Popularidad: cantidad / maxCantidad del listado; 0 si ninguna tiene resenias.
    /// Sucursales sin resenias quedan con puntaje 0 (valores nulos/ausentes tratados como 0).
    /// Desempate determinista: mayor puntaje combinado, luego mayor puntuacion, luego mayor
    /// cantidad de resenias y finalmente id ascendente.

    private List<SucursalMiniDTO> ordenarSucursalesPorPuntuacionYPopularidad(
            List<SucursalMiniDTO> sucursales) {

        long maxCantidad = sucursales.stream()
                .mapToLong(s -> s.getCantidadPuntuaciones() == null ? 0L : s.getCantidadPuntuaciones())
                .max()
                .orElse(0L);

        sucursales.sort(java.util.Comparator
                .comparingDouble((SucursalMiniDTO s) -> puntajeCombinadoSucursal(s, maxCantidad))
                .reversed()
                .thenComparing(java.util.Comparator.comparingDouble(
                        (SucursalMiniDTO s) -> puntuacionNormalizada(s)).reversed())
                .thenComparing(java.util.Comparator.comparingLong(
                        (SucursalMiniDTO s) -> cantidadSegura(s)).reversed())
                .thenComparing(SucursalMiniDTO::getId, java.util.Comparator.nullsLast(Integer::compareTo)));

        return sucursales;
    }


    private static double puntajeCombinadoSucursal(SucursalMiniDTO sucursal, long maxCantidad) {

        double puntuacionNorm = puntuacionNormalizada(sucursal);
        double popularidadNorm = maxCantidad > 0 ? (double) cantidadSegura(sucursal) / maxCantidad : 0.0;
        return 0.5 * puntuacionNorm + 0.5 * popularidadNorm;
    }


    private static double puntuacionNormalizada(SucursalMiniDTO sucursal) {

        if (sucursal.getPuntuacion() == null || cantidadSegura(sucursal) == 0L) {
            return 0.0;
        }
        return (sucursal.getPuntuacion() - 1.0) / 4.0;
    }


    private static long cantidadSegura(SucursalMiniDTO sucursal) {

        return sucursal.getCantidadPuntuaciones() == null ? 0L : sucursal.getCantidadPuntuaciones();
    }


    // ver si hago endpoint para listar sucursales eliminadas (no creo)


    public SucursalResponseDTO verSucursalPorId(Integer id) {

        // Consulta anterior a la optimización. Reemplazada por findByIdConDetalle.
        // Sucursal sucursal = listarSucursalPorId(id);
        Sucursal sucursal = sucRepo.findByIdConDetalle(id)
                .orElseThrow(() -> new NotFoundException("No se encontró la sucursal"));

        return mapearSucursal(sucursal);
    }


    private SucursalResponseDTO mapearSucursal(Sucursal suc) {

        SucursalResponseDTO dto = new SucursalResponseDTO();

        dto.setId(suc.getId());

        dto.setNombre(suc.getNombre());

        dto.setDireccion(suc.getDireccion());

        dto.setTelefono(suc.getTelefono());

        dto.setDescripcion(suc.getDescripcion());

        dto.setFechaCreacion(suc.getFechaCreacion());

        dto.setCategoria(suc.getCategoria().getCategoria().name());

        dto.setHoraApertura(suc.getHoraApertura());

        dto.setHoraCierre(suc.getHoraCierre());

        dto.setDiasApertura(DiasSemanaUtil.aNombres(suc.getDiasApertura()));

        dto.setFotoPerfil(suc.getFotoPerfil() == null ? null : suc.getFotoPerfil().getFotoValida());

        dto.setLatitud(suc.getLatitud());
        dto.setLongitud(suc.getLongitud());
        dto.setGooglePlaceId(suc.getGooglePlaceId());

        // Consulta anterior a la optimización. Reemplazada por getPuntuacionesSucursales.
        // dto.setPuntuacion(reseniaService.getPuntuacionPromedioSucursal(suc.getId()));
        // dto.setCantidadPuntuaciones(reseniaService.getPuntuacionesTotalesSucursal(suc.getId()));
        Map<Integer, ResumenPuntuacion> puntuaciones =
                reseniaService.getPuntuacionesSucursales(List.of(suc.getId()));
        dto.setPuntuacion(puntuacionDe(puntuaciones.get(suc.getId())));
        dto.setCantidadPuntuaciones(cantidadDe(puntuaciones.get(suc.getId())));

        // Se usa la sucursal ya cargada (con su empleador en el EntityGraph) en
        // vez de volver a buscarla dentro de listarImagenesPorSucursal.
        // Consulta anterior a la optimización. Reemplazada por listarImagenesPorSucursal de ImagenService.
        // List<ImagenResponseDTO> imagenes = listarImagenesPorSucursal(suc.getId(),
        //         suc.getEmpleador().getEmail());
        List<ImagenResponseDTO> imagenes = imagenService.listarImagenesPorSucursal(suc);
        dto.setImagenes(imagenes);

        UsuarioMiniDTO empleador = new UsuarioMiniDTO(
                suc.getEmpleador().getNombre(),
                suc.getEmpleador().getApellido()
        );
        dto.setEmpleador(empleador);

        Set<UsuarioMiniDTO> empleadosDTO = suc.getEmpleados()
                .stream()
                .map(u -> new UsuarioMiniDTO(
                        u.getNombre(),
                        u.getApellido()
                ))
                .collect(Collectors.toSet());
        dto.setEmpleados(empleadosDTO);

        return dto;
    }


    public List<SucursalMiniDTO> filtrarListaSucursales(ECategoria categoria,
                                                        String nombre) {

        String nombreFiltro = (nombre == null || nombre.isBlank()) ? null : nombre;

        return mapearSucursalesMini(sucRepo.findOperativas(categoria, nombreFiltro));
    }


    public List<SucursalMiniDTO> listarSucursalesPropias(String userEmail){

        return mapearSucursalesMini(sucRepo.findSucursalesDondeEsta(userEmail));
    }


    public List<SucursalMiniDTO> listarSucursalesComoEmpleado(String userEmail) {

        return mapearSucursalesMini(sucRepo.findSucursalesDondeEsEmpleado(userEmail));
    }


    public Sucursal listarSucursalPorId(Integer id) {
        // Consulta anterior a la optimización. Reemplazada por findByIdConEmpleador.
        // return sucRepo.findById(id).
        //         orElseThrow(() -> new NotFoundException("No se encontró la sucursal"));
        return sucRepo.findByIdConEmpleador(id).
                orElseThrow(() -> new NotFoundException("No se encontró la sucursal"));
    }


    public List<EmpleadoResponseDTO> verEmpleadosParaElegir(Integer sucursalId){

        Sucursal suc = listarSucursalPorId(sucursalId);

        // ver si falta valid

        Set<Usuario> empleados = suc.getEmpleados();

        // Las fotos y las puntuaciones de todos los empleados en una sola query
        // cada una, en vez de tres por empleado.
        Map<Integer, Imagen> fotos = imagenesDe(empleados);

        Map<Integer, ResumenPuntuacion> puntuaciones =
                reseniaService.getPuntuacionesEmpleados(
                        empleados.stream().map(Usuario::getId).toList(),
                        suc.getId());

        return empleados.stream().
                map(emp -> new EmpleadoResponseDTO(
                        emp.getId(), emp.getNombre(), emp.getApellido(),
                        FotoPerfilUsuario.resolver(fotos.get(emp.getId())),
                        puntuacionDe(puntuaciones.get(emp.getId())),
                        cantidadDe(puntuaciones.get(emp.getId()))
                )).toList();
    }


    /// Trae las fotos de perfil de varios usuarios en una sola query.
    /// El id de fotoPerfil se puede leer sin cargarlo, asi que no dispara queries.

    private Map<Integer, Imagen> imagenesDe(Set<Usuario> usuarios) {

        Map<Integer, Integer> fotoIdPorUsuario = new java.util.HashMap<>();
        List<Integer> ids = new java.util.ArrayList<>();

        for (Usuario usuario : usuarios) {
            Imagen foto = usuario.getFotoPerfil();
            if (foto == null || foto.getId() == null) {
                continue;
            }
            fotoIdPorUsuario.put(usuario.getId(), foto.getId());
            ids.add(foto.getId());
        }

        if (ids.isEmpty()) return Map.of();

        Map<Integer, Imagen> imagenesPorId = imagenRepo.findAllById(ids.stream().distinct().toList())
                .stream()
                .collect(Collectors.toMap(Imagen::getId, img -> img, (a, b) -> a));

        Map<Integer, Imagen> resultado = new java.util.HashMap<>();
        for (Map.Entry<Integer, Integer> entry : fotoIdPorUsuario.entrySet()) {
            Imagen imagen = imagenesPorId.get(entry.getValue());
            if (imagen != null) {
                resultado.put(entry.getKey(), imagen);
            }
        }

        return resultado;
    }


    public Set<UsuarioResponseDTO> verEmpleados(Integer sucursalId, String userEmail) {

        Sucursal suc = listarSucursalPorId(sucursalId);

        Usuario usuario = usuarioService.listarUsuarioPorEmail(userEmail);

        if (!usuarioService.esAdmin(usuario) &&
                !esEmpleadorAca(suc, userEmail)) {
            throw new ForbiddenException("No tenes permisos");
        }

        Set<Usuario> empleados = suc.getEmpleados();

        Map<Integer, Imagen> fotos = imagenesDe(empleados);

        return empleados.stream().
                map(u -> new UsuarioResponseDTO(u.getId(), u.getNombre(),
                        u.getApellido(), u.getEmail(),
                        FotoPerfilUsuario.resolver(fotos.get(u.getId()))))
                .collect(Collectors.toSet());
    }


    public void agregarEmpleado(Integer sucursalId, UsuarioEmailDTO userEmail,
                                String empleadorEmail) {

        if(userEmail.getEmail().equals(empleadorEmail)){
            throw new BadRequestException("No te podes agregar a vos mismo como empleado");
        }

        Sucursal sucursal = listarSucursalPorId(sucursalId);

        if (!esEmpleadorAca(sucursal, empleadorEmail)) {
            throw new ForbiddenException("No tenes permisos");
        }

        Usuario empleado = usuarioService.listarUsuarioPorEmail(userEmail.getEmail());

        if(sucursal.getEmpleados().contains(empleado)){
            throw new ConflictException("El usuario ya es empleado de la sucursal");
        }

        gestionEmpleadoService.enviarSolicitud(empleado, sucursal);
    }


    public void eliminarEmpleado(Integer sucursalId, Integer empleadoId,
                                 String empleadorEmail) {

        Sucursal sucursal = listarSucursalPorId(sucursalId);

        if (!esEmpleadorAca(sucursal, empleadorEmail)) {
            throw new ForbiddenException("No tenes permisos");
        }

        Usuario empleado = usuarioService.listarUsuarioPorId(empleadoId);

        if(!sucursal.getEmpleados().contains(empleado)){
            throw new ConflictException("El usuario no es empleado de la sucursal");
        }

        sucursal.getEmpleados().remove(empleado);

        sucRepo.save(sucursal);
    }


    @Transactional
    public void convertirmeEnEmpleado(Integer sucursalId, String empleadorEmail){

        Sucursal suc = listarSucursalPorId(sucursalId);

        if(!esEmpleadorAca(suc, empleadorEmail)){
            throw new ForbiddenException("No tenes permisos");
        }

        Usuario empleador = usuarioService.listarUsuarioPorEmail(empleadorEmail);

        if(suc.getEmpleados().contains(empleador)){
            throw new ConflictException("Ya sos empleado en la sucursal");
        }

        Rol rolEmpleado = rolService.listarPorRol(ERol.EMPLEADO);

        if(!empleador.getRoles().contains(rolEmpleado)){
            usuarioService.agregarRol(empleador, rolEmpleado);
        }

        suc.getEmpleados().add(empleador);
        sucRepo.save(suc);
    }


    public boolean esEmpleadorAca(Sucursal sucursal, String empleadorEmail) {

        return sucursal.getEmpleador().getEmail().equals(empleadorEmail);
    }


    public void guardarEmpleado(Usuario empleadoNuevo, Sucursal sucursal){

        // ver si valido algo mas
        sucursal.getEmpleados().add(empleadoNuevo);

        sucRepo.save(sucursal);
    }


}



