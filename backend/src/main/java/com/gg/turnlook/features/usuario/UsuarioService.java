package com.gg.turnlook.features.usuario;

import com.gg.turnlook.common.util.DiasSemanaUtil;

import com.gg.turnlook.auth.dto.LoginDTO;
import com.gg.turnlook.common.exception.ConflictException;
import com.gg.turnlook.common.exception.RegistroPendienteVerificacionException;
import com.gg.turnlook.common.exception.ForbiddenException;
import com.gg.turnlook.common.exception.NotFoundException;
import com.gg.turnlook.common.exception.UnauthorizedException;
import com.gg.turnlook.features.imagen.ImagenService;
import com.gg.turnlook.features.resenia.ReseniaService;
import com.gg.turnlook.features.resenia.ReseniaService.ResumenPuntuacion;
import com.gg.turnlook.features.sucursal.Sucursal;
import com.gg.turnlook.features.sucursal.dto.SucursalMiniDTO;
import com.gg.turnlook.features.usuario.dto.UsuarioCrearDTO;
import com.gg.turnlook.features.usuario.dto.UsuarioEmailDTO;
import com.gg.turnlook.features.usuario.dto.UsuarioMeDTO;
import com.gg.turnlook.features.usuario.dto.UsuarioMiniAdminDTO;
import com.gg.turnlook.features.usuario.dto.UsuarioModificarDTO;
import com.gg.turnlook.features.usuario.dto.UsuarioPerfilResponseDTO;
import com.gg.turnlook.features.usuario.dto.UsuarioResponseDTO;
import com.gg.turnlook.features.usuario.rol.ERol;
import com.gg.turnlook.features.usuario.rol.Rol;
import com.gg.turnlook.features.usuario.rol.RolService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;




@Service
public class UsuarioService {


    // Limite del listado administrativo. Se mantiene el retorno List para no
    // cambiar el contrato de la API, pero se evita traer todos los usuarios.
    private static final int LIMITE_USUARIOS_ADMIN = 500;


    private final UsuarioRepository usRepo;
    private final RolService rolService;
    private final ImagenService imagenService;
    private final ReseniaService reseniaService;
    private final PasswordEncoder passEncoder;
    private final com.gg.turnlook.auth.refresh.RefreshTokenRepository refreshTokens;
    private final com.gg.turnlook.auth.password.PasswordResetTokenRepository resetTokens;
    private final com.gg.turnlook.auth.BloqueoUsuario bloqueo;
    private final com.gg.turnlook.features.verificacion.VerificacionRepository verificaciones;


    public UsuarioService(UsuarioRepository usRepo, RolService rolService, ImagenService imagenService, ReseniaService reseniaService, PasswordEncoder passEncoder, com.gg.turnlook.auth.refresh.RefreshTokenRepository refreshTokens, com.gg.turnlook.auth.password.PasswordResetTokenRepository resetTokens, com.gg.turnlook.auth.BloqueoUsuario bloqueo, com.gg.turnlook.features.verificacion.VerificacionRepository verificaciones) {
        this.usRepo = usRepo;
        this.rolService = rolService;
        this.imagenService = imagenService;
        this.reseniaService = reseniaService;
        this.passEncoder = passEncoder;
        this.refreshTokens = refreshTokens;
        this.resetTokens = resetTokens;
        this.bloqueo = bloqueo;
        this.verificaciones = verificaciones;

    }


    /// METODOS


    @org.springframework.transaction.annotation.Transactional
    public void recuperarCuenta(LoginDTO login) {

        Usuario usuario = usRepo.findByEmailAndActivoFalse(login.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Credenciales incorrectas"));

        if (!passEncoder.matches(login.getPass(), usuario.getPassword())) {
            throw new UnauthorizedException("Credenciales incorrectas");
        }

        throw new ForbiddenException("La reactivación de cuentas deshabilitadas requiere revisión administrativa");
    }


    public Set<String> setRolesComoString(Usuario u) {
        return u.getRoles().stream()
                .map(r -> r.getRol().name())
                .collect(Collectors.toSet());
    }


    @org.springframework.transaction.annotation.Transactional
    public Usuario crearUsuario(UsuarioCrearDTO u) {

        u.setEmail(u.getEmail().trim().toLowerCase(java.util.Locale.ROOT));
        if (usRepo.existsByEmail(u.getEmail())) {
            // Solo informamos el estado pendiente tras comprobar la contraseña.
            Usuario existente = usRepo.findByEmailAndActivoTrue(u.getEmail()).orElse(null);
            if (existente != null && !existente.isEmailVerificado()
                    && passEncoder.matches(u.getPassword(), existente.getPassword())) {
                throw new RegistroPendienteVerificacionException();
            }
            throw new ConflictException("El email ingresado ya está en uso");
        }
        Rol rol = rolService.listarPorRol(ERol.CLIENTE);

        Usuario user = new Usuario(u.getNombre(), u.getApellido(), u.getEmail(),
                passEncoder.encode(u.getPassword()));

        user.getRoles().add(rol);
        user.setFotoPerfil(imagenService.crearFotoPerfil(u.getFotoUrl(), u.getFotoPublicId()));

        return usRepo.save(user);
    }


    @org.springframework.transaction.annotation.Transactional
    public void eliminarUsuario(Integer userId, String userEmail) {

        Usuario admin = listarUsuarioPorEmail(userEmail);

        if (Objects.equals(userId, admin.getId())) {
            throw new ConflictException("No podes eliminar tu cuenta siendo Administrador");
        }

        Usuario u = listarUsuarioPorId(userId);

        u = bloqueo.bloquear(u.getId());
        revocarSesiones(u);
        u.setActivo(false);

        usRepo.save(u);
    }


    @org.springframework.transaction.annotation.Transactional
    public void borrarCuentaPropia(String userEmail) {

        Usuario usuario = listarUsuarioPorEmail(userEmail);

        usuario = bloqueo.bloquear(usuario.getId());
        revocarSesiones(usuario);
        usuario.setActivo(false);

        usRepo.save(usuario);
    }


    @org.springframework.transaction.annotation.Transactional
    public void modificarUsuario(UsuarioModificarDTO usuario, Integer userId, String userEmail) {

        Usuario activo = listarUsuarioPorEmail(userEmail);

        Usuario user = bloqueo.bloquear(userId);

        if (!esAdmin(activo) &&
                !Objects.equals(activo.getId(), user.getId())) {
            throw new ForbiddenException("No tenes permisos");
        }

        if (usuario.getNombre() != null && !usuario.getNombre().isBlank()) user.setNombre(usuario.getNombre());

        if (usuario.getApellido() != null && !usuario.getApellido().isBlank()) user.setApellido(usuario.getApellido());

        if (usuario.getEmail() != null) usuario.setEmail(usuario.getEmail().trim().toLowerCase(java.util.Locale.ROOT));
        if (usuario.getEmail() != null && !usuario.getEmail().equals(user.getEmail())) {
            if (usRepo.existsByEmail(usuario.getEmail())) {
                throw new ConflictException("El email ingresado ya esta en uso");
            }
            user.setEmail(usuario.getEmail().trim().toLowerCase(java.util.Locale.ROOT));
            user.setEmailVerificado(false);
            revocarSesiones(user);
        }

        if (usuario.getPassword() != null) {
            throw new com.gg.turnlook.common.exception.BadRequestException("Usá /auth/change-password para cambiar la contraseña");
        }

        if (usuario.getFotoUrl() != null && !usuario.getFotoUrl().isBlank()) {
            imagenService.cambiarFotoPerfilUsuario(user, usuario.getFotoUrl(), usuario.getFotoPublicId());
        }

        usRepo.save(user);
    }


    // Consulta anterior a la optimización. Reemplazada por buscarMiniAdmin.
    // public List<UsuarioMiniAdminDTO> listarUsuarios() {
    //     return usRepo.findAll().stream()
    //             .map(u -> new UsuarioMiniAdminDTO(
    //                     u.getId(), u.getNombre(), u.getApellido(), u.isActivo()))
    //             .toList();
    // }
    public List<UsuarioMiniAdminDTO> listarUsuarios() {
        return usRepo.buscarMiniAdmin(null, null, null,
                PageRequest.of(0, LIMITE_USUARIOS_ADMIN));
    }


    // Consulta anterior a la optimización. Reemplazada por buscarMiniAdmin.
    // public List<UsuarioMiniAdminDTO> filtrarListaUsuarios(String nombre, String apellido, Boolean activo) {
    //     return listarUsuarios().stream()
    //             .filter(u -> nombre == null || u.getNombre().equalsIgnoreCase(nombre))
    //             .filter(u -> apellido == null || u.getApellido().equalsIgnoreCase(apellido))
    //             .filter(u -> activo == null || u.isEstado() == activo)
    //             .toList();
    // }
    public List<UsuarioMiniAdminDTO> filtrarListaUsuarios(String nombre, String apellido, Boolean activo) {
        return usRepo.buscarMiniAdmin(nombre, apellido, activo,
                PageRequest.of(0, LIMITE_USUARIOS_ADMIN));
    }


    public UsuarioResponseDTO listarUsuariosPorEmail(UsuarioEmailDTO email) {

        Usuario u = listarUsuarioPorEmail(email.getEmail());

        return new UsuarioResponseDTO(u.getId(), u.getNombre(), u.getApellido()
                , u.getEmail(), u.getFotoPerfil() == null ? null : u.getFotoPerfil().getFotoValida());
    }


    public Usuario listarUsuarioPorEmail(String email) {
        return usRepo.findByEmailAndActivoTrue(email).
                orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }


    public Usuario listarUsuarioPorId(Integer id) {
        return usRepo.findById(id).
                orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }


    // Variante para validaciones que necesitan los roles sin cargar el resto
    // de las relaciones del usuario.
    public Usuario listarUsuarioPorIdConRoles(Integer id) {
        return usRepo.findByIdConRoles(id).
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
        // Consulta anterior a la optimización. Reemplazada por findByIdConPerfil.
        // Usuario u = listarUsuarioPorId(userId);
        Usuario u = usRepo.findByIdConPerfil(userId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        UsuarioPerfilResponseDTO uPerfil = new UsuarioPerfilResponseDTO();

        uPerfil.setId(u.getId());

        uPerfil.setNombre(u.getNombre());

        uPerfil.setApellido(u.getApellido());

        uPerfil.setEmail(u.getEmail());

        uPerfil.setFechaCreacion(u.getFechaCreacion());

        uPerfil.setRoles(setRolesComoString(u));

        uPerfil.setFotoPerfil(u.getFotoPerfil() == null ? null : u.getFotoPerfil().getFotoValida());

        // Las puntuaciones de todas las sucursales del usuario en una sola
        // query, en vez de dos por sucursal.
        Map<Integer, ResumenPuntuacion> puntuaciones =
                reseniaService.getPuntuacionesSucursales(
                        u.getSucursalesEmpleado().stream().map(Sucursal::getId).toList());

        uPerfil.setSucursalesEmpleado(u.getSucursalesEmpleado().stream()
                .map(suc -> new SucursalMiniDTO(suc.getId(), suc.getNombre(),
                        suc.getCategoria().getCategoria().name(),
                        suc.getFotoPerfil() == null ? null : suc.getFotoPerfil().getFotoValida(),
                        puntuacionDe(puntuaciones.get(suc.getId())),
                        cantidadDe(puntuaciones.get(suc.getId())),
                        DiasSemanaUtil.aNombres(suc.getDiasApertura()))
                )
                .toList());

        return uPerfil;
    }


    private static Double puntuacionDe(ResumenPuntuacion resumen) {

        return resumen == null ? null : resumen.promedioConUnDecimal();
    }


    private static Long cantidadDe(ResumenPuntuacion resumen) {

        return resumen == null ? 0L : resumen.cantidad();
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
                usuario.getFotoPerfil() == null ? null : usuario.getFotoPerfil().getFotoValida());
    }


    private void revocarSesiones(Usuario usuario) {
        usuario.setCredencialesVersion(usuario.getCredencialesVersion() + 1);
        refreshTokens.revocarPorUsuario(usuario.getId());
        resetTokens.revocarPorUsuario(usuario.getId());
        verificaciones.invalidarPorUsuario(usuario.getId(), java.time.Instant.now());
    }

    public boolean esAdmin(Usuario usuario) {
        Set<String> roles = setRolesComoString(usuario);
        return roles != null && roles.contains(ERol.ADMINISTRADOR.name());
    }


}




