package com.gg.turnlook.Backend.Security;


import com.gg.turnlook.Backend.Model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;


@Service
public class JwtService {


    @Value("${jwt.secret}")
    private String secret;



    /// METODOS



    private Key getSignKey(){
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }


    public String generarToken(Usuario usuario){

        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("id",  usuario.getId())
                .claim("roles", usuario.getRoles().stream().map(r -> r.getNombre()).toList())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000))
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
