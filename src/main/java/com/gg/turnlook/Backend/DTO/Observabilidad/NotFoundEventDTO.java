package com.gg.turnlook.Backend.DTO.Observabilidad;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NotFoundEventDTO(
        @NotBlank
        @Size(max = 1024)
        @Pattern(regexp = "^/(?!/).*", message = "La ruta debe ser relativa al sitio")
        String path,
        @Size(max = 2048)
        String referrer
) {
}
