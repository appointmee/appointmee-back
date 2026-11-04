package com.gg.turnlook.features.resenia;

import com.gg.turnlook.features.resenia.dto.ReseniaCrearDTO;
import com.gg.turnlook.features.resenia.dto.ReseniaResponseDTO;
import com.gg.turnlook.features.turno.Turno;
import java.util.List;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;


@Service
public class ReseniaService {


    private final ReseniaRepository reseniaRepo;


    public ReseniaService(ReseniaRepository reseniaRepo) {
        this.reseniaRepo = reseniaRepo;
    }


    /// METODOS


    public void guardarResenia(ReseniaCrearDTO reseniaDto, Turno turno) {

        Resenia reseniaFinal = new Resenia(
                reseniaDto.getPuntuacion(), reseniaDto.getComentario(), turno);

        reseniaRepo.save(reseniaFinal);
    }


    public Double getPuntuacionPromedioEmpleado(Integer empleadoId, Integer sucursalId) {

        Double promedio = reseniaRepo.promedioPuntuacionesEmpleado(empleadoId, sucursalId);

        return promedio == null ? null : Math.round(promedio * 10.0) / 10.0;
    }


    public Double getPuntuacionPromedioSucursal(Integer sucursalId) {

        Double promedio = reseniaRepo.promedioPuntuacionesSucursal(sucursalId);

        return promedio == null ? null : Math.round(promedio * 10.0) / 10.0;
    }


    public Double getPuntuacionPromedioServicio(Integer servicioId) {

        Double promedio = reseniaRepo.promedioPuntuacionesServicio(servicioId);

        return promedio == null ? null : Math.round(promedio * 10.0) / 10.0;
    }


    public Long getPuntuacionesTotalesEmpleado(Integer empleadoId, Integer sucursalId) {

        return reseniaRepo.puntuacionesTotalesEmpleado(empleadoId, sucursalId);
    }


    public Long getPuntuacionesTotalesSucursal(Integer sucursalId) {

        return reseniaRepo.puntuacionesTotalesSucursal(sucursalId);
    }


    /// Promedio con un decimal y cantidad de resenias de varias sucursales,
    /// en una sola query. Las sucursales sin resenias no aparecen en el mapa.

    public Map<Integer, ResumenPuntuacion> getPuntuacionesSucursales(
            List<Integer> sucursalIds) {

        return aMapa(resumenPuntuacionesPorSucursal(sucursalIds), sucursalIds);
    }


    private List<Object[]> resumenPuntuacionesPorSucursal(List<Integer> ids) {

        if (ids == null || ids.isEmpty()) return List.of();

        return reseniaRepo.resumenPuntuacionesPorSucursal(ids);
    }


    /// Promedio redondeado y total de resenias de una sucursal.
    /// El promedio va en null cuando la sucursal todavia no tiene resenias.

    public record ResumenPuntuacion(Double promedio, Long cantidad) {


        public Double promedioConUnDecimal() {
            return promedio == null ? null : Math.round(promedio * 10.0) / 10.0;
        }
    }


    /// Igual que getPuntuacionesSucursales pero para los servicios de una lista.

    public Map<Integer, ResumenPuntuacion> getPuntuacionesServicios(
            List<Integer> servicioIds) {

        return aMapa(resumenPuntuacionesPorServicio(servicioIds), servicioIds);
    }


    /// Igual que getPuntuacionesSucursales pero para los empleados de una lista.
    /// La puntuacion de cada empleado se calcula solo con sus turnos de la
    /// sucursal indicada, no con todos sus turnos en otras sucursales.

    public Map<Integer, ResumenPuntuacion> getPuntuacionesEmpleados(
            List<Integer> empleadoIds, Integer sucursalId) {

        return aMapa(
                resumenPuntuacionesPorEmpleado(empleadoIds, sucursalId),
                empleadoIds);
    }


    private List<Object[]> resumenPuntuacionesPorServicio(List<Integer> ids) {

        if (ids == null || ids.isEmpty()) return List.of();

        return reseniaRepo.resumenPuntuacionesPorServicio(ids);
    }


    private List<Object[]> resumenPuntuacionesPorEmpleado(
            List<Integer> ids, Integer sucursalId) {

        if (ids == null || ids.isEmpty()) return List.of();

        return reseniaRepo.resumenPuntuacionesPorEmpleado(ids, sucursalId);
    }


    private Map<Integer, ResumenPuntuacion> aMapa(List<Object[]> filas,
                                                 List<Integer> ids) {

        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }

        return filas.stream()
                .collect(Collectors.toMap(
                        fila -> ((Number) fila[0]).intValue(),
                        fila -> new ResumenPuntuacion(
                                ((Number) fila[1]).doubleValue(),
                                ((Number) fila[2]).longValue()),
                        (a, b) -> a));
    }


    public Long getPuntuacionesTotalesServicio(Integer servicioId) {

        return reseniaRepo.puntuacionesTotalesServicio(servicioId);
    }


    public List<ReseniaResponseDTO> getReseniasPorSucursal(Integer sucursalId) {

        return reseniaRepo.findBySucursalId(sucursalId).stream()
                .map(r -> new ReseniaResponseDTO(
                        r.getPuntuacion(),
                        r.getComentario(),
                        r.getFechaResenia()))
                .toList();
    }


}








