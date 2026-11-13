package com.gg.turnlook.auth.jwt;
import com.gg.turnlook.features.usuario.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import lombok.RequiredArgsConstructor;
@Component @RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepo;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // Public credential flows must work even when the browser sends an expired access token.
        String path = request.getServletPath();
        if (java.util.Set.of("/auth/login", "/auth/register", "/auth/refresh", "/auth/forgot-password",
                "/auth/reset-password", "/auth/reset-password/validate", "/auth/send-verification", "/auth/verify-email", "/auth/verification-status", "/auth/complete-verification", "/usuarios/inicio-sesion", "/usuarios/crear").contains(path)) {
            chain.doFilter(request, response); return;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) { chain.doFilter(request, response); return; }
        try {
            Claims claims = jwtService.validarAccessToken(header.substring(7).trim());
            Usuario usuario = usuarioRepo.findByEmailAndActivoTrue(claims.getSubject())
                    .orElseThrow(() -> new JwtException("Usuario no disponible"));
            if (!usuario.isEmailVerificado()) throw new JwtException("Email no verificado");
            if (!usuario.getId().equals(claims.get("id", Integer.class)) ||
                    !Integer.valueOf(usuario.getCredencialesVersion()).equals(claims.get("version", Integer.class))) {
                throw new JwtException("Sesión revocada");
            }
            var roles = usuario.getRoles().stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getRol().name())).toList();
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(usuario.getEmail(), null, roles));
        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Token inválido, vencido o revocado\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
