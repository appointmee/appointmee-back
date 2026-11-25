package com.gg.turnlook.features.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MensajeCrearDTO {

    @NotBlank(message = "El mensaje no puede estar vacío")
    @Size(max = 2000, message = "El mensaje no puede superar los 2000 caracteres")
    private String contenido;

    /** Identificador idempotente generado por el cliente. */
    @NotNull(message = "Falta el identificador del mensaje")
    private UUID clienteMensajeId;
}
