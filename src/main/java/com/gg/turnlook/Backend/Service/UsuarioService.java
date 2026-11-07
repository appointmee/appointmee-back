package com.gg.turnlook.Backend.Service;

import com.gg.turnlook.Backend.DTO.Usuario.LoginDTO;
import com.gg.turnlook.Backend.DTO.Sucursal.SucursalMiniDTO;
import com.gg.turnlook.Backend.DTO.Usuario.*;
import com.gg.turnlook.Backend.Enum.ERol;
import com.gg.turnlook.Backend.Excepciones.ConflictException;
import com.gg.turnlook.Backend.Excepciones.ForbiddenException;
import com.gg.turnlook.Backend.Excepciones.NotFoundException;
import com.gg.turnlook.Backend.Excepciones.UnauthorizedException;
import com.gg.turnlook.Backend.Model.Rol;
import com.gg.turnlook.Backend.Model.Usuario;
import com.gg.turnlook.Backend.Repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class UsuarioService {


    private final UsuarioRepository usRepo;
    private final RolService rolService;
    private final ImagenService imagenService;
    private final ReseniaService reseniaService;
    private final PasswordEncoder passEncoder;


    public UsuarioService(UsuarioRepository usRepo, RolService rolService, ImagenService imagenService, ReseniaService reseniaService, PasswordEncoder passEncoder) {
        this.usRepo = usRepo;
        this.rolService = rolService;
        this.imagenService = imagenService;
        this.reseniaService = reseniaService;
        this.passEncoder = passEncoder;

    }


    /// METODOS


    public void activarCuenta(LoginDTO login) {

        Usuario usuario = usRepo.findByEmailAndActivoFalse(login.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Credenciales incorrectas"));

        if (!passEncoder.matches(login.getPass(), usuario.getPassword())) {
            throw new UnauthorizedException("Credenciales incorrectas");
        }

        usuario.setActivo(true);
        usRepo.save(usuario);
    }


    public Set<String> setRolesComoString(Usuario u) {
        return u.getRoles().stream()
                .map(r -> r.getRol().name())
                .collect(Collectors.toSet());
    }


    public Usuario crearUsuario(UsuarioCrearDTO u) {

        Rol rol = rolService.listarPorRol(ERol.CLIENTE);

        Usuario user = new Usuario(u.getNombre(), u.getApellido(), u.getEmail(),
                passEncoder.encode(u.getPassword()));

        user.getRoles().add(rol);
        user.setFotoPerfil(imagenService.crearFotoPerfil(u.getFotoUrl()));

        return usRepo.save(user);
    }


    public void eliminarUsuario(Integer userId, String userEmail) {

        Usuario admin = listarUsuarioPorEmail(userEmail);

        if (Objects.equals(userId, admin.getId())) {
            throw new ConflictException("No podes eliminar tu cuenta siendo Administrador");
        }

        Usuario u = listarUsuarioPorId(userId);

        u.setActivo(false);

        usRepo.save(u);
    }


    public void borrarCuentaPropia(String userEmail) {

        Usuario usuario = listarUsuarioPorEmail(userEmail);

        usuario.setActivo(false);

        usRepo.save(usuario);
    }


    public void modificarUsuario(UsuarioModificarDTO usuario, Integer userId, String userEmail) {

        Usuario activo = listarUsuarioPorEmail(userEmail);

        Usuario user = listarUsuarioPorId(userId);

        if (!esAdmin(activo) &&
                !Objects.equals(activo.getId(), user.getId())) {
            throw new ForbiddenException("No tenes permisos");
        }

        if (usuario.getNombre() != null && !usuario.getNombre().isBlank()) user.setNombre(usuario.getNombre());

        if (usuario.getApellido() != null && !usuario.getApellido().isBlank()) user.setApellido(usuario.getApellido());

        if (usuario.getEmail() != null && !usuario.getEmail().equals(user.getEmail())) {
            if (usRepo.existsByEmail(usuario.getEmail())) {
                throw new ConflictException("El email ingresado ya esta en uso");
            }
            user.setEmail(usuario.getEmail());
        }

        if (usuario.getPassword() != null) user.setPassword(passEncoder.encode(usuario.getPassword()));

        if (usuario.getFotoUrl() != null && !usuario.getFotoUrl().isBlank()) {
            imagenService.cambiarFotoPerfilUsuario(user, usuario.getFotoUrl());
        }

        usRepo.save(user);
    }


    public List<UsuarioMiniAdminDTO> listarUsuarios() {
        return usRepo.findAll().stream()
                .map(u -> new UsuarioMiniAdminDTO(
                        u.getId(), u.getNombre(), u.getApellido(), u.isActivo()))
                .toList();
    }


    public List<UsuarioMiniAdminDTO> filtrarListaUsuarios(String nombre, String apellido, Boolean activo) {
        return listarUsuarios().stream()
                .filter(u -> nombre == null || u.getNombre().equalsIgnoreCase(nombre))
                .filter(u -> apellido == null || u.getApellido().equalsIgnoreCase(apellido))
                .filter(u -> activo == null || u.isEstado() == activo)
                .toList();
    }


    public UsuarioResponseDTO listarUsuariosPorEmail(UsuarioEmailDTO email) {

        Usuario u = listarUsuarioPorEmail(email.getEmail());

        return new UsuarioResponseDTO(u.getId(), u.getNombre(), u.getApellido()
                , u.getEmail(), u.getFotoPerfil().getFotoValida());
    }


    public Usuario listarUsuarioPorEmail(String email) {
        return usRepo.findByEmailAndActivoTrue(email).
                orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }


    public Usuario listarUsuarioPorId(Integer id) {
        return usRepo.findById(id).
                orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }


    public UsuarioPerfilResponseDTO verPerfilPorId(Integer userId, String userEmail) {

        Usuario activo = listarUsuarioPorEmail(userEmail);

        if (!esAdmin(activo) &&
                !Objects.equals(activo.getId(), userId)) {
            throw new ForbiddenException("No tenes permisos");
        }

        return mapearUsuario(userId);
    }


    private UsuarioPerfilResponseDTO mapearUsuario(Integer userId) {
        Usuario u = listarUsuarioPorId(userId);
        UsuarioPerfilResponseDTO uPerfil = new UsuarioPerfilResponseDTO();

        uPerfil.setId(u.getId());

        uPerfil.setNombre(u.getNombre());

        uPerfil.setApellido(u.getApellido());

        uPerfil.setEmail(u.getEmail());

        uPerfil.setFechaCreacion(u.getFechaCreacion());

        uPerfil.setRoles(setRolesComoString(u));

        uPerfil.setFotoPerfil(u.getFotoPerfil().getFotoValida());

        uPerfil.setSucursalesEmpleado(u.getSucursalesEmpleado().stream()
                .map(suc -> new SucursalMiniDTO(suc.getId(), suc.getNombre(),
                        suc.getCategoria().getCategoria().name(),
                        suc.getFotoPerfil().getFotoValida(),
                        reseniaService.getPuntuacionPromedioSucursal(suc.getId()),
                        reseniaService.getPuntuacionesTotalesSucursal(suc.getId()))
                )
                .toList());

        return uPerfil;
    }


    public void agregarRol(Usuario usuario, Rol rol) {

        usuario.getRoles().add(rol);

        usRepo.save(usuario);
    }


    public void eliminarFotoPerfil(String userEmail) {

        Usuario usuario = listarUsuarioPorEmail(userEmail);

        imagenService.eliminarFotoPerfilUsuario(usuario);
    }


    public UsuarioMeDTO getMe(String userEmail) {

        Usuario usuario = listarUsuarioPorEmail(userEmail);

        return new UsuarioMeDTO(usuario.getId(), setRolesComoString(usuario),
                usuario.getFotoPerfil().getFotoValida());
    }


    public boolean esAdmin(Usuario usuario) {
        Set<String> roles = setRolesComoString(usuario);
        return roles != null && roles.contains(ERol.ADMINISTRADOR.name());
    }


}




