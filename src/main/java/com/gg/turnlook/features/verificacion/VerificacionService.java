package com.gg.turnlook.features.verificacion;

import com.gg.turnlook.features.verificacion.dto.SolicitarCodigoRequest;
import com.gg.turnlook.features.verificacion.dto.VerificarCodigoRequest;
import com.gg.turnlook.features.verificacion.enums.CanalVerificacion;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class VerificacionService

{

    private final VerificacionRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();
    private final List<CanalEnvioService> canales;


    public void solicitarCodigo(SolicitarCodigoRequest request){

        validarDestinatario(request.canal(), request.destinatario());

        Instant ahora = Instant.now();

        repository
                .findFirstByCanalAndDestinatarioOrderByFechaCreacionDesc(
                        request.canal(),
                        request.destinatario())
                .ifPresent(ultima -> {
                    Instant proximoEnvio = ultima.getFechaCreacion()
                            .plusSeconds(60);

                    if (ahora.isBefore(proximoEnvio)) {
                        throw new ResponseStatusException(
                                HttpStatus.TOO_MANY_REQUESTS,
                                "Debés esperar un minuto antes de solicitar otro código");
                    }
                });

        CanalEnvioService canal = obtenerCanal(request.canal());
        String codigo = generarCodigo();
        String hash = passwordEncoder.encode(codigo);


        Verificacion verificacion = Verificacion.builder()
                .canal(request.canal())
                .destinatario(request.destinatario())
                .codigoHash(passwordEncoder.encode(codigo))
                .fechaCreacion(Instant.now())
                .fechaVencimiento(Instant.now().plusSeconds(300))
                .intentosFallidos(0)
                .build();

        repository.save(verificacion);

        canal.enviarCodigo(request.destinatario(), codigo);



    }

    public void verificarCodigo(VerificarCodigoRequest request) {
        validarDestinatario(request.canal(), request.destinatario());

        Verificacion verificacion = repository
                .findFirstByCanalAndDestinatarioOrderByFechaCreacionDesc(
                        request.canal(),
                        request.destinatario())
                .orElseThrow(() ->
                        new IllegalArgumentException("Código inválido"));

        if (verificacion.getFechaUso() != null) {
            throw new IllegalArgumentException("El código ya fue utilizado");
        }

        if (!Instant.now().isBefore(verificacion.getFechaVencimiento())) {
            throw new IllegalArgumentException("El código venció");
        }

        if (verificacion.getIntentosFallidos() >= 5) {
            throw new IllegalArgumentException("Superaste los intentos permitidos");
        }

        if (!passwordEncoder.matches(
                request.codigo(), verificacion.getCodigoHash())) {

            verificacion.setIntentosFallidos(
                    verificacion.getIntentosFallidos() + 1);

            repository.save(verificacion);

            throw new IllegalArgumentException("Código inválido");
        }

        verificacion.setFechaUso(Instant.now());
        repository.save(verificacion);

        // Aquí actualizar el usuario para marcar su email o teléfono como verificado.
    }


    private String generarCodigo() {
        return String.format(
                Locale.ROOT, "%06d", random.nextInt(1_000_000));
    }
    private void validarDestinatario(
            CanalVerificacion canal,
            String destinatario) {

        if (canal == null) {
            throw new IllegalArgumentException("El canal es obligatorio");
        }

        if (destinatario == null || destinatario.isBlank()) {
            throw new IllegalArgumentException("El destinatario es obligatorio");
        }

        switch (canal) {
            case EMAIL -> {
                if (!destinatario.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                    throw new IllegalArgumentException("Email inválido");
                }
            }

            case WHATSAPP -> {
                if (!destinatario.matches("^\\+[1-9]\\d{1,14}$")) {
                    throw new IllegalArgumentException(
                            "Usá un teléfono internacional, por ejemplo +5491112345678");
                }
            }

            default -> throw new IllegalArgumentException(
                    "No hay validación para este canal");
        }
    }

    private CanalEnvioService obtenerCanal(CanalVerificacion canal) {
        return canales.stream()
                .filter(servicio -> servicio.getCanal() == canal)
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Canal no disponible"));
    }
}

