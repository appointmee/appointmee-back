package com.gg.turnlook.Backend.Security;



import com.gg.turnlook.Backend.Repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;


@Component
public class JwtFilter extends OncePerRequestFilter {


    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepo;



    public JwtFilter(JwtService jwtService, UsuarioRepository usuarioRepo) {
        this.jwtService = jwtService;
        this.usuarioRepo = usuarioRepo;
    }


    ///  METODOS


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        String email;
        try {
             email = jwtService.extraerEmail(token);
        } catch (Exception e) {
            filterChain.doFilter(request, response);
            return;
        }

        List<String> roles = jwtService.extraerRoles(token);
        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol))
                .toList();

        if(jwtService.tokenValido(token)){

            if(usuarioRepo.existsByEmailAndActivoFalse(email)){

                response.sendError(401, "La cuenta está desactivada");
                return;
            }

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            email, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
