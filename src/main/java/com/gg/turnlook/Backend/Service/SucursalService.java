package com.gg.turnlook.Backend.Service;


import com.gg.turnlook.Backend.DTO.Imagen.ImagenDTO;
import com.gg.turnlook.Backend.DTO.Imagen.ImagenResponseDTO;
import com.gg.turnlook.Backend.DTO.Sucursal.SucursalCrearDTO;
import com.gg.turnlook.Backend.DTO.Sucursal.SucursalMiniDTO;
import com.gg.turnlook.Backend.DTO.Sucursal.SucursalModificarDTO;
import com.gg.turnlook.Backend.DTO.Sucursal.SucursalResponseDTO;
import com.gg.turnlook.Backend.DTO.Usuario.EmpleadoResponseDTO;
import com.gg.turnlook.Backend.DTO.Usuario.UsuarioEmailDTO;
import com.gg.turnlook.Backend.DTO.Usuario.UsuarioResponseDTO;
import com.gg.turnlook.Backend.DTO.Usuario.UsuarioMiniDTO;
import com.gg.turnlook.Backend.Enum.ECategoria;
import com.gg.turnlook.Backend.Enum.ERol;
import com.gg.turnlook.Backend.Excepciones.BadRequestException;
import com.gg.turnlook.Backend.Excepciones.ConflictException;
import com.gg.turnlook.Backend.Excepciones.ForbiddenException;
import com.gg.turnlook.Backend.Excepciones.NotFoundException;
import com.gg.turnlook.Backend.Model.*;
import com.gg.turnlook.Backend.Repository.CategoriaRepository;
import com.gg.turnlook.Backend.Repository.SucursalRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class SucursalService {


    private final SucursalRepository sucRepo;
    private final CategoriaRepository catRepo;
    private final UsuarioService usuarioService;
    private final ImagenService imagenService;
    private final ReseniaService reseniaService;
    private final GestionEmpleadoService gestionEmpleadoService;
    private final RolService rolService;



    public SucursalService(SucursalRepository sucRepo, CategoriaRepository catRepo, UsuarioService usuarioService, ImagenService imagenService, ReseniaService reseniaService, GestionEmpleadoService gestionEmpleadoService, RolService rolService) {
        this.sucRepo = sucRepo;
        this.catRepo = catRepo;
        this.usuarioService = usuarioService;
        this.imagenService = imagenService;
        this.reseniaService = reseniaService;
        this.gestionEmpleadoService = gestionEmpleadoService;
        this.rolService = rolService;
    }


    /// METODOS


    public boolean enSucursal(String userEmail, Sucursal sucursal) {

        return sucursal.getEmpleador().getEmail().equals(userEmail) ||
                sucursal.getEmpleados().stream()
                        .anyMatch(u -> u.getEmail().equals(userEmail));
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

        suc.setFotoPerfil(imagenService.crearFotoPerfil(sucursal.getFotoUrl()));

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

        if (!suc.getHoraCierre().isAfter(suc.getHoraApertura())) {
            throw new BadRequestException("El horario de cierre tiene que ser posterior al de apertura");
        }

        // ver si valida bien como ultimo dsp
        if (sucursal.getFotoUrl() != null && !sucursal.getFotoUrl().isBlank()) {
            imagenService.cambiarFotoPerfilSucursal(suc, sucursal.getFotoUrl());
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

        return sucRepo.findByActivoTrue().stream()
                .map(suc -> new SucursalMiniDTO(suc.getId(), suc.getNombre(),
                        suc.getCategoria().getCategoria().name(),
                        suc.getFotoPerfil().getFotoValida(),
                        reseniaService.getPuntuacionPromedioSucursal(suc.getId()),
                        reseniaService.getPuntuacionesTotalesSucursal(suc.getId()))
                )
                .toList();
    }


    // ver si hago endpoint para listar sucursales eliminadas (no creo)


    public SucursalResponseDTO verSucursalPorId(Integer id) {

        Sucursal sucursal = listarSucursalPorId(id);

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

        dto.setFotoPerfil(suc.getFotoPerfil().getFotoValida());

        dto.setPuntuacion(reseniaService.getPuntuacionPromedioSucursal(suc.getId()));

        dto.setCantidadPuntuaciones(reseniaService.getPuntuacionesTotalesSucursal(suc.getId()));

        List<ImagenResponseDTO> imagenes = listarImagenesPorSucursal(suc.getId(),
                suc.getEmpleador().getEmail());
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

        return sucursales.stream().map(s -> new SucursalMiniDTO(
                        s.getId(), s.getNombre(), s.getCategoria().getCategoria().name(),
                        s.getFotoPerfil().getFotoValida(),
                        reseniaService.getPuntuacionPromedioSucursal(s.getId()),
                        reseniaService.getPuntuacionesTotalesSucursal(s.getId())))
                .toList();
    }


    public List<SucursalMiniDTO> listarSucursalesPropias(String userEmail){

        return sucRepo.findSucursalesDondeEsta(userEmail)
                .stream()
                .map(suc -> new SucursalMiniDTO(suc.getId(), suc.getNombre(),
                        suc.getCategoria().getCategoria().name(),
                        suc.getFotoPerfil().getFotoValida(),
                        reseniaService.getPuntuacionPromedioSucursal(suc.getId()),
                        reseniaService.getPuntuacionesTotalesSucursal(suc.getId()))
                )
                .toList();
    }


    public Sucursal listarSucursalPorId(Integer id) {
        return sucRepo.findById(id).
                orElseThrow(() -> new NotFoundException("No se encontró la sucursal"));
    }


    public List<EmpleadoResponseDTO> verEmpleadosParaElegir(Integer sucursalId){

        Sucursal suc = listarSucursalPorId(sucursalId);

        // ver si falta valid

        return suc.getEmpleados().stream().
                map(emp -> new EmpleadoResponseDTO(
                        emp.getId(), emp.getNombre(), emp.getApellido(),
                        emp.getFotoPerfil().getFotoValida(),
                        reseniaService.getPuntuacionPromedioEmpleado(emp.getId()),
                        reseniaService.getPuntuacionesTotalesEmpleado(emp.getId())
                )).toList();
    }


    public Set<UsuarioResponseDTO> verEmpleados(Integer sucursalId, String userEmail) {

        Sucursal suc = listarSucursalPorId(sucursalId);

        Usuario usuario = usuarioService.listarUsuarioPorEmail(userEmail);

        if (!usuarioService.esAdmin(usuario) &&
                !esEmpleadorAca(suc, userEmail)) {
            throw new ForbiddenException("No tenes permisos");
        }

        return suc.getEmpleados().stream().
                map(u -> new UsuarioResponseDTO(u.getId(), u.getNombre(),
                        u.getApellido(), u.getEmail(), u.getFotoPerfil().getFotoValida()))
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



