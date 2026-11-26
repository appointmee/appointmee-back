package com.gg.turnlook.features.solicitud.empleador.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;




@Data
public class SolicitudEmpleadorCrearDTO {


    @NotBlank(message = "El motivo es obligatorio")
    @Size(min = 20, max = 3000,
            message = "El motivo debe tener entre 20 y 3000 caracteres")
    private String motivo;

    /// Toda solicitud debe incluir al menos una ubicacion elegida en Places.
    /// No se impone un maximo arbitrario: cada ubicacion se valida y la URL
    /// completa queda limitada por el servidor.
    @Valid
    @NotEmpty(message = "Debe incluir al menos una ubicacion")
    private List<@Valid UbicacionSolicitudDTO> ubicaciones = new ArrayList<>();


}



