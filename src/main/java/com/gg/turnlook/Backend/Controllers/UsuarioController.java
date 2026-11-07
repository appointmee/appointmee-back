package com.gg.turnlook.Backend.Controllers;



import com.gg.turnlook.Backend.DTO.Usuario.*;
import com.gg.turnlook.Backend.Model.Usuario;
import com.gg.turnlook.Backend.Security.JwtService;
import com.gg.turnlook.Backend.Service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;





@RestController
@RequestMapping("/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {



    private final UsuarioService usuarioService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;



    public UsuarioController(UsuarioService usuarioService, AuthenticationManager authenticationManager, JwtService jwtService) {
        this.usuarioService = usuarioService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }



    /// ENDPOINTS



    // ver si lo muevo o lo dejo asi
    @PostMapping("/inicio-sesion")
    public ResponseEntity<?> iniciarSesion(@Valid @RequestBody LoginDTO login) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(login.getEmail(), login.getPass()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        Usuario usuario = usuarioService.listarUsuarioPorEmail(userDetails.getUsername());

        return ResponseEntity.ok().body(
                new LoginResponseDTO(jwtService.generarToken(usuario)));
    }


    @PostMapping("/recuperar-cuenta")
    public ResponseEntity<?> recuperarCuenta(@Valid @RequestBody LoginDTO login){

        usuarioService.recuperarCuenta(login);
        return ResponseEntity.ok().body("Su cuenta fue activada correctamente. " +
                "Ya puede iniciar sesion");
    }


    @GetMapping("/me")
    public ResponseEntity<UsuarioMeDTO> getMe(@AuthenticationPrincipal String userEmail){

        return ResponseEntity.ok().body(usuarioService.getMe(userEmail));
    }


    @PostMapping("/crear")
    public ResponseEntity<?> crearUsuario(@Valid @RequestBody UsuarioCrearDTO u) {

        usuarioService.crearUsuario(u);
        return ResponseEntity.ok().body("Se creó al usuario");
    }


    @PatchMapping("/modificar/{userId}")
    public ResponseEntity<?> modificarUsuario(@PathVariable("userId") Integer userId,
                                              @Valid @RequestBody UsuarioModificarDTO usuario,
                                              @AuthenticationPrincipal String userEmail) {

        usuarioService.modificarUsuario(usuario, userId, userEmail);
        return ResponseEntity.ok().body("Usuario modificado");
    }


    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @DeleteMapping("/eliminar/{userId}")
    public ResponseEntity<?> eliminarUsuario(@PathVariable("userId") Integer userId,
                                             @AuthenticationPrincipal String userEmail) {

        usuarioService.eliminarUsuario(userId, userEmail);
        return ResponseEntity.ok().body("Se eliminó al usuario");
    }


    @PreAuthorize("hasRole('CLIENTE')")
    @DeleteMapping("/borrar-cuenta")
    public ResponseEntity<?> borrarUsuario(@AuthenticationPrincipal String userEmail) {

        usuarioService.borrarCuentaPropia(userEmail);
        return ResponseEntity.ok().body("Se eliminó la cuenta");
    }


    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @GetMapping("/listar")
    public ResponseEntity<?> listarUsuarios() {

        return ResponseEntity.ok().body(usuarioService.listarUsuarios());
    }


    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @GetMapping("/listar/filtrar")
    public ResponseEntity<?> filtrarListaUsuarios(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String apellido,
            @RequestParam(required = false) Boolean activo) {

        return ResponseEntity.ok().body(usuarioService.filtrarListaUsuarios(
                nombre, apellido, activo));
    }


    @GetMapping("/{userId}")
    public ResponseEntity<?> filtrarUsuariosId(@PathVariable("userId") Integer userId,
                                               @AuthenticationPrincipal String userEmail) {

        return ResponseEntity.ok().body(usuarioService.verPerfilPorId(userId, userEmail));
    }


    // ver q hago con este o lo dejo asi
    @PreAuthorize("hasAnyRole('EMPLEADOR','ADMINISTRADOR')")
    @GetMapping("/email")
    public ResponseEntity<?> filtrarUsuariosEmail(@Valid @RequestBody UsuarioEmailDTO userEmail,
                                                  Authentication auth) {

        return ResponseEntity.ok().body(usuarioService.listarUsuariosPorEmail(userEmail));
    }


    @DeleteMapping("/eliminar/foto-perfil")
    public ResponseEntity<?> eliminarFotoPerfil(@AuthenticationPrincipal String email){

        usuarioService.eliminarFotoPerfil(email);
        return ResponseEntity.ok().body("Se eliminó la foto de perfil");
    }
}
