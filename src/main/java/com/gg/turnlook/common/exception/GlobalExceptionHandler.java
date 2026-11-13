package com.gg.turnlook.common.exception;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;




@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleJsonInvalido(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("message", "La solicitud no tiene un formato válido"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> manejarErroresValidacion(MethodArgumentNotValidException ex) {

        Map<String, String> errores = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errores.put(error.getField(), error.getDefaultMessage());
        });

        return ResponseEntity.status(400).body(errores);
    }


    @ExceptionHandler(EmailNoVerificadoException.class)
    public ResponseEntity<?> handleEmailNoVerificado(EmailNoVerificadoException e) {
        return ResponseEntity.status(403).body(Map.of(
                "code", "EMAIL_NOT_VERIFIED",
                "message", e.getMessage(),
                "verificationToken", e.getVerificationToken(),
                "verificationExpiresIn", 900));
    }

    @ExceptionHandler(CodigoVerificacionException.class)
    public ResponseEntity<?> handleCodigoVerificacion(CodigoVerificacionException e) {
        return ResponseEntity.status("EMAIL_ALREADY_VERIFIED".equals(e.getCode()) ? 409 : 400).body(Map.of(
                "code", e.getCode(), "message", e.getMessage(),
                "attemptsRemaining", e.getEstado().attemptsRemaining(),
                "retryAfterSeconds", e.getEstado().retryAfterSeconds()));
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
        if (e instanceof RegistroPendienteVerificacionException) {
            return ResponseEntity.status(409).body(java.util.Map.of(
                    "code", "REGISTRATION_PENDING_VERIFICATION", "message", e.getMessage()));
        }
        return ResponseEntity.status(409).body(e.getMessage());
    }


    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<?> handleBadRequest(BadRequestException e){
        if (e instanceof PasswordChangeException change) {
            return ResponseEntity.badRequest().body(Map.of("code", change.getCode(), "message", change.getMessage()));
        }
        return ResponseEntity.status(400).body(e.getMessage());
    }


    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentials(BadCredentialsException e){
        return ResponseEntity.status(401).body("Credenciales incorrectas");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleImagenDemasiadoGrande(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(413).body("La imagen no puede superar los 5 MB");
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneral(Exception e){
        e.printStackTrace(); // dsp sacar
        return ResponseEntity.status(500).body("Error interno del servidor");
    }


}
