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

        Categoria cat = catRepo.findById(sucursal.getCategoriaId()).
                orElseThrow(() -> new NotFoundException("No se encontró la categoria"));

        Usuario empleador = usuarioService.listarUsuarioPorEmail(empleadorEmail);

        Sucursal suc = new Sucursal(sucursal.getNombre(), sucursal.getDireccion(),
                sucursal.getTelefono(), sucursal.getDescripcion(),
                sucursal.getHoraApertura(), sucursal.getHoraCierre(),
                cat, empleador);

        suc.setDiasAperturaPorNombre(DiasSemanaUtil.aDias(sucursal.getDiasApertura()));

        suc.setFotoPerfil(imagenService.crearFotoPerfil(sucursal.getFotoUrl(), sucursal.getFotoPublicId()));

        sucRepo.save(suc);
    }


    public void modificarSucursal(SucursalModificarDTO sucursal,
                                  Integer sucursalId, String empleadorEmail) {

        Sucursal suc = listarSucursalPorId(sucursalId);

        if (!suc.getEmpleador().getEmail().equals(empleadorEmail)) {
            throw new ForbiddenException("No tenes permisos");
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

        if (!suc.getHoraCierre().isAfter(suc.getHoraApertura())) {
            throw new BadRequestException("El horario de cierre tiene que ser posterior al de apertura");
        }

        // ver si valida bien como ultimo dsp
        if (sucursal.getFotoUrl() != null && !sucursal.getFotoUrl().isBlank()) {
            imagenService.cambiarFotoPerfilSucursal(suc, sucursal.getFotoUrl(), sucursal.getFotoPublicId());
        }

        sucRepo.save(suc);
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

        sucursal.setActivo(false);

        sucRepo.save(sucursal);
    }


    public void borrarSucursalPropia(Integer sucursalId, String empleadorEmail) {

        Sucursal sucursal = listarSucursalPorId(sucursalId);

        if (!esEmpleadorAca(sucursal, empleadorEmail)) {
            throw new ForbiddenException("No tenes permisos");
        }

        sucursal.setActivo(false);

        sucRepo.save(sucursal);
    }


    public List<SucursalMiniDTO> listarSucursales() {

        return mapearSucursalesMini(sucRepo.findByActivoTrue());
    }


    /// Mapea una lista de sucursales a DTOs trayendo las puntuaciones de todas
    /// en una sola query. La categoria y la foto ya vienen cargadas por el
    /// @EntityGraph del repository, asi que no hay queries por sucursal.

    private List<SucursalMiniDTO> mapearSucursalesMini(List<Sucursal> sucursales) {

        Map<Integer, ResumenPuntuacion> puntuaciones =
                reseniaService.getPuntuacionesSucursales(
                        sucursales.stream().map(Sucursal::getId).toList());

        List<SucursalMiniDTO> resultado = new java.util.ArrayList<>(sucursales.stream()
                .map(suc -> new SucursalMiniDTO(
                        suc.getId(), suc.getNombre(),
                        suc.getCategoria().getCategoria().name(),
                        suc.getFotoPerfil() == null ? null : suc.getFotoPerfil().getFotoValida(),
                        puntuacionDe(puntuaciones.get(suc.getId())),
                        cantidadDe(puntuaciones.get(suc.getId())),
                        DiasSemanaUtil.aNombres(suc.getDiasApertura()))
                )
                .toList());

        return ordenarSucursalesPorPuntuacionYPopularidad(resultado);
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

        List<Sucursal> sucursales;

        boolean tieneNombre = nombre != null && !nombre.isBlank();

        if (categoria != null && tieneNombre) {
            sucursales =
                    sucRepo.findByNombreContainingIgnoreCaseAndCategoriaCategoriaAndActivoTrue(
                            nombre, categoria);
        } else if (categoria == null && tieneNombre) {
            sucursales = sucRepo.findByNombreContainingIgnoreCaseAndActivoTrue(nombre);
        } else if (categoria != null) {
            sucursales = sucRepo.findByCategoriaCategoriaAndActivoTrue(categoria);
        } else {
            return listarSucursales();
        }

        return mapearSucursalesMini(sucursales);
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
                        fotos.get(emp.getId()) != null
                                ? fotos.get(emp.getId()).getFotoValida() : null,
                        puntuacionDe(puntuaciones.get(emp.getId())),
                        cantidadDe(puntuaciones.get(emp.getId()))
                )).toList();
    }


    /// Trae las fotos de perfil de varios usuarios en una sola query.
    /// El id de fotoPerfil se puede leer sin cargarlo, asi que no dispara queries.

    private Map<Integer, Imagen> imagenesDe(Set<Usuario> usuarios) {

        Map<Integer, Integer> imagenIdPorUsuarioId = usuarios.stream()
                .filter(usuario -> usuario.getFotoPerfil() != null)
                .filter(usuario -> usuario.getFotoPerfil().getId() != null)
                .collect(Collectors.toMap(
                        Usuario::getId,
                        usuario -> usuario.getFotoPerfil().getId(),
                        (primeraImagen, imagenDuplicada) -> primeraImagen));

        if (imagenIdPorUsuarioId.isEmpty()) return Map.of();

        Map<Integer, Imagen> imagenesPorId = imagenRepo
                .findAllById(imagenIdPorUsuarioId.values().stream().distinct().toList())
                .stream()
                .collect(Collectors.toMap(Imagen::getId, imagen -> imagen, (a, b) -> a));

        return imagenIdPorUsuarioId.entrySet().stream()
                .filter(entry -> imagenesPorId.containsKey(entry.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> imagenesPorId.get(entry.getValue()),
                        (a, b) -> a));
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
                        fotos.get(u.getId()) != null
                                ? fotos.get(u.getId()).getFotoValida() : null))
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


