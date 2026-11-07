package com.gg.turnlook.Backend.Service;


import com.gg.turnlook.Backend.DTO.Resenia.ReseniaCrearDTO;
import com.gg.turnlook.Backend.DTO.Resenia.ReseniaResponseDTO;
import com.gg.turnlook.Backend.Model.Resenia;
import com.gg.turnlook.Backend.Model.Turno;
import com.gg.turnlook.Backend.Repository.ReseniaRepository;
import org.springframework.stereotype.Service;

import java.util.List;


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


    public Integer getPuntuacionPromedioEmpleado(Integer empleadoId) {

        Double promedio = reseniaRepo.promedioPuntuacionesEmpleado(empleadoId);

        return promedio != null ? (int) Math.round(promedio) : null;
    }


    public Integer getPuntuacionPromedioSucursal(Integer sucursalId) {

        Double promedio = reseniaRepo.promedioPuntuacionesSucursal(sucursalId);

        return promedio != null ? (int) Math.round(promedio) : null;
    }


    public Integer getPuntuacionPromedioServicio(Integer servicioId) {

        Double promedio = reseniaRepo.promedioPuntuacionesServicio(servicioId);

        return promedio != null ? (int) Math.round(promedio) : null;
    }


    public Long getPuntuacionesTotalesEmpleado(Integer empleadoId) {

        return reseniaRepo.puntuacionesTotalesEmpleado(empleadoId);
    }


    public Long getPuntuacionesTotalesSucursal(Integer sucursalId) {

        return reseniaRepo.puntuacionesTotalesSucursal(sucursalId);
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








