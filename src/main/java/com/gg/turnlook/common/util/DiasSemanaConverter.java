package com.gg.turnlook.common.util;


import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;


// Los dias de apertura de la sucursal se guardan en una sola columna de texto
// con los nombres separados por comas: "MONDAY,TUESDAY,WEDNESDAY".
// Siempre se guardan en orden lunes a domingo para que el valor sea estable.

@Converter
public class DiasSemanaConverter implements AttributeConverter<Set<DayOfWeek>, String> {


    private static final String SEPARADOR = ",";


    @Override
    public String convertToDatabaseColumn(Set<DayOfWeek> dias) {

        if (dias == null || dias.isEmpty()) return null;

        return DiasSemanaUtil.ORDEN_SEMANA.stream()
                .filter(dias::contains)
                .map(DayOfWeek::name)
                .collect(Collectors.joining(SEPARADOR));
    }


    @Override
    public Set<DayOfWeek> convertToEntityAttribute(String columna) {

        if (columna == null || columna.isBlank()) return new LinkedHashSet<>();

        // si alguien edito la fila a mano en la base y dejo un dia raro, se ignora
        return Arrays.stream(columna.split(SEPARADOR))
                .map(String::trim)
                .map(DiasSemanaUtil::aDia)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }


}
