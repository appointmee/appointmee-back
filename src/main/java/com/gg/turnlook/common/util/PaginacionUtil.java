package com.gg.turnlook.common.util;

import com.gg.turnlook.common.exception.BadRequestException;

/** Reglas compartidas de paginacion de los listados de esta historia. */
public final class PaginacionUtil {

    /** Tamano unico de pagina para todos los listados completos. */
    public static final int TAMANO_PAGINA = 10;

    private PaginacionUtil() {
    }

    /** Parte de cero; los numeros recibidos como texto responden 400. */
    public static int pagina(String valor) {
        if (valor == null || valor.isBlank()) {
            return 0;
        }
        try {
            int numero = Integer.parseInt(valor.trim());
            if (numero < 0) {
                throw new BadRequestException("El parámetro page no puede ser negativo");
            }
            return numero;
        } catch (NumberFormatException e) {
            throw new BadRequestException(
                    "El parámetro page debe ser un número entero");
        }
    }

}
