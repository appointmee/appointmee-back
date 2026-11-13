package com.gg.turnlook.auth.jwt;

import com.gg.turnlook.features.usuario.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;




@Service
public class JwtService {


    @Value("${jwt.secret}")
    private String secret;



    @Value("${jwt.access-ttl-seconds:900}")
    private long ttlSeconds;

    public long getTtlSeconds() { return ttlSeconds; }

    /// METODOS



    private Key getSignKey(){
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }


    public String generarToken(Usuario usuario){

        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("id", usuario.getId())
                .claim("type", "access")
                .claim("version", usuario.getCredencialesVersion())
                .claim("roles", usuario.getRoles().stream().map(r -> r.getNombre()).toList())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlSeconds * 1000))
                .signWith(getSignKey())
                .compact();
    }


    private Claims leerTokenClaims(String token){

        return Jwts.parser()
                .verifyWith((SecretKey) getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    public String generarTokenVerificacion(Usuario usuario) {
        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("id", usuario.getId())
                .claim("version", usuario.getCredencialesVersion())
                .claim("type", "email-verification")
                .id(java.util.UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900_000))
                .signWith(getSignKey()).compact();
    }

    public Claims validarTokenVerificacion(String token) {
        Claims claims = leerTokenClaims(token);
        if (!"email-verification".equals(claims.get("type", String.class))) {
            throw new io.jsonwebtoken.JwtException("Tipo de token inválido");
        }
        return claims;
    }

    public Claims validarAccessToken(String token) {
        Claims claims = leerTokenClaims(token);
        if (!"access".equals(claims.get("type", String.class))) {
            throw new io.jsonwebtoken.JwtException("Tipo de token inválido");
        }
        return claims;
    }

    public String extraerEmail(String token){

        return leerTokenClaims(token).getSubject();
    }


    public Integer extraerId(String token){

        return leerTokenClaims(token).get("id",  Integer.class);
    }


    public List<String> extraerRoles(String token){

        return leerTokenClaims(token).get("roles", List.class);
    }


    public boolean tokenValido(String token){

        return !estaExpirado(token);
    }


    public boolean estaExpirado(String token){

        return leerTokenClaims(token).getExpiration().before(new Date());
    }

}
