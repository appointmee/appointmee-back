package com.gg.turnlook.common.util;


import com.gg.turnlook.common.exception.BadRequestException;

import java.time.DayOfWeek;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


// Utilidades para convertir los dias de la semana de una sucursal entre el enum
// DayOfWeek (base de datos / entidad) y los Strings que viajan por la API.

public final class DiasSemanaUtil {


    /// Orden de la semana usado siempre para mostrar y devolver los dias (lunes a domingo).

    public static final List<DayOfWeek> ORDEN_SEMANA = List.of(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
    );


    /// Nombres aceptados en la API: el del enum en ingles y su alias en castellano.

    private static final Map<String, DayOfWeek> NOMBRES = Map.ofEntries(
            Map.entry("MONDAY", DayOfWeek.MONDAY),
            Map.entry("LUNES", DayOfWeek.MONDAY),
            Map.entry("TUESDAY", DayOfWeek.TUESDAY),
            Map.entry("MARTES", DayOfWeek.TUESDAY),
            Map.entry("WEDNESDAY", DayOfWeek.WEDNESDAY),
            Map.entry("MIERCOLES", DayOfWeek.WEDNESDAY),
            Map.entry("MIÉRCOLES", DayOfWeek.WEDNESDAY),
            Map.entry("THURSDAY", DayOfWeek.THURSDAY),
            Map.entry("JUEVES", DayOfWeek.THURSDAY),
            Map.entry("FRIDAY", DayOfWeek.FRIDAY),
            Map.entry("VIERNES", DayOfWeek.FRIDAY),
            Map.entry("SATURDAY", DayOfWeek.SATURDAY),
            Map.entry("SABADO", DayOfWeek.SATURDAY),
            Map.entry("SÁBADO", DayOfWeek.SATURDAY),
            Map.entry("SUNDAY", DayOfWeek.SUNDAY),
            Map.entry("DOMINGO", DayOfWeek.SUNDAY)
    );


    private DiasSemanaUtil() {
    }


    /// Convierte un nombre de dia en DayOfWeek, o null si el nombre no es valido.
    /// No tira excepcion: se usa al leer la columna de la base, que puede tener
    /// valores raros si alguien la edito a mano desde el panel.

    public static DayOfWeek aDia(String nombre) {

        if (nombre == null || nombre.isBlank()) return null;

        return NOMBRES.get(nombre.trim().toUpperCase(Locale.ROOT));
    }


    /// Convierte los nombres de los dias (MONDAY, LUNES, lunes, etc.) en un Set de DayOfWeek.

    public static Set<DayOfWeek> aDias(List<String> nombres) {

        if (nombres == null || nombres.isEmpty()) {
            throw new BadRequestException("Debe seleccionar al menos un dia de apertura");
        }

        Set<DayOfWeek> dias = new LinkedHashSet<>();

        for (String nombre : nombres) {

            if (nombre == null || nombre.isBlank()) {
                throw new BadRequestException("El dia de apertura no puede estar vacio");
            }

            DayOfWeek dia = NOMBRES.get(nombre.trim().toUpperCase(Locale.ROOT));

            if (dia == null) {
                throw new BadRequestException("El dia de apertura '" + nombre + "' no es valido");
            }

            dias.add(dia);
        }

        if (dias.isEmpty()) {
            throw new BadRequestException("Debe seleccionar al menos un dia de apertura");
        }

        return dias;
    }


    /// Devuelve los dias de apertura ordenados de lunes a domingo.
    /// Si la sucursal todavia no tiene dias cargados se devuelven los 7 (abre todos los dias).

    public static List<String> aNombres(Set<DayOfWeek> dias) {

        if (dias == null || dias.isEmpty()) {
            return ORDEN_SEMANA.stream().map(DayOfWeek::name).toList();
        }

        return ORDEN_SEMANA.stream()
                .filter(dias::contains)
                .map(DayOfWeek::name)
                .toList();
    }


}
