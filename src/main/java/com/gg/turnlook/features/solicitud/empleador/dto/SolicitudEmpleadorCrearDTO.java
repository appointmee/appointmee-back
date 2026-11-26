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

    /// Toda solicitud debe incluir al menos una ubicacion elegida en Places y
    /// como maximo 10. El limite se valida aqui (HTTP 400) y tambien en el
    /// servicio para no depender exclusivamente de la capa web.
    @Valid
    @NotEmpty(message = "Debe incluir al menos una ubicacion")
    @Size(max = 10, message = "Una solicitud puede incluir como maximo 10 ubicaciones")
    private List<@Valid UbicacionSolicitudDTO> ubicaciones = new ArrayList<>();


}



