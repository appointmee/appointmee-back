package com.gg.turnlook.Backend.Excepciones;


import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> manejarErroresValidacion(MethodArgumentNotValidException ex) {

        Map<String, String> errores = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errores.put(error.getField(), error.getDefaultMessage());
        });

        return ResponseEntity.status(400).body(errores);
    }


    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<?> handleNotFound(NotFoundException e){
        return ResponseEntity.status(404).body(e.getMessage());
    }


    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<?> handleUnauthorized(UnauthorizedException e){
        return ResponseEntity.status(401).body(e.getMessage());
    }


    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<?> handleForbidden(ForbiddenException e){
        return ResponseEntity.status(403).body(e.getMessage());
    }


    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<?> handleAuthorizationDenied(AuthorizationDeniedException e){
        return ResponseEntity.status(403).body("No tenes permisos");
    }


    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<?> handleConflict(ConflictException e){
        return ResponseEntity.status(409).body(e.getMessage());
    }


    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<?> handleBadRequest(BadRequestException e){
        return ResponseEntity.status(400).body(e.getMessage());
    }


    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentials(BadCredentialsException e){
        return ResponseEntity.status(401).body("Credenciales incorrectas");
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneral(Exception e){
        e.printStackTrace(); // dsp sacar
        return ResponseEntity.status(500).body("Error interno del servidor");
    }


}
