package com.gg.turnlook.Backend.config;


import com.gg.turnlook.Backend.Security.JwtAccessDeniedHandler;
import com.gg.turnlook.Backend.Security.JwtAuthenticationEntryPoint;
import com.gg.turnlook.Backend.Security.JwtFilter;
import com.gg.turnlook.Backend.Security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {


    private final JwtFilter jwtFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;


    public SecurityConfig(JwtFilter jwtFilter, JwtAuthenticationEntryPoint authenticationEntryPoint, JwtAccessDeniedHandler accessDeniedHandler) {
        this.jwtFilter = jwtFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) {

        return httpSecurity
                .csrf(csrf -> csrf.disable())
                .cors(cors ->
                        cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/redireccion.js",
                                "/admin-folder/**",
                                "/configuracion-folder/**",
                                "/creditos-folder/**",
                                "/detalle-mis-turnos-folder/**",
                                "/empleador-folder/**",
                                "/empleado-folder/**",
                                "/error-folder/**",
                                "/formularioempleador-transformarse-folder/**",
                                "/index-folder/**",
                                "/login-folder/**",
                                "/misturnos-folder/**",
                                "/pago-folder/**",
                                "/PerdisteCuenta-folder/**",
                                "/perfil-folder/**",
                                "/recursos/**",
                                "/register-folder/**",
                                "/sucursal-folder/**",
                                "/turnos-folder/**",
                                "/mis-sucursales-folder/**",

                                "/usuarios/inicio-sesion",
                                "/usuarios/recuperar-cuenta",
                                "/usuarios/crear",

                                "/pagos/webhook",  // prueba rapida fast flash

                                // para swagger
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html")
                        .permitAll()
                        .anyRequest().authenticated())

                .exceptionHandling(e -> e
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))

                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                "http://127.0.0.1:3000",
                "http://localhost:3000",
                "http://127.0.0.1:8080",
                "http://localhost:8080",
                "https://appointmee-vcs2.onrender.com"));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }


    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            CustomUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);

        authProvider.setPasswordEncoder(passwordEncoder);

        return authProvider;
    }


    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configur)
            throws Exception {

        return configur.getAuthenticationManager();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
