package com.gg.turnlook.Backend.Security;



import com.gg.turnlook.Backend.Excepciones.NotFoundException;
import com.gg.turnlook.Backend.Model.Usuario;
import com.gg.turnlook.Backend.Service.UsuarioService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;



@Service
public class CustomUserDetailsService implements UserDetailsService {


    private final UsuarioService usuarioService;



    public CustomUserDetailsService(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }



    /// METODOS


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        Usuario usuario;

       try {
           usuario = usuarioService.listarUsuarioPorEmail(email);
       }catch (NotFoundException e){
           throw new UsernameNotFoundException("Usuario no encontrado");
       }

        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getPassword())
                .roles(usuarioService.setRolesComoString(usuario).toArray(String[]::new))
                .build();
    }
}

