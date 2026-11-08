package com.gg.turnlook.features.servicio;

import com.gg.turnlook.common.exception.BadRequestException;
import com.gg.turnlook.common.exception.ForbiddenException;
import com.gg.turnlook.common.exception.NotFoundException;
import com.gg.turnlook.features.imagen.ImagenService;
import com.gg.turnlook.features.resenia.ReseniaService;
import com.gg.turnlook.features.resenia.ReseniaService.ResumenPuntuacion;
import com.gg.turnlook.features.servicio.dto.ServicioCrearDTO;
import com.gg.turnlook.features.servicio.dto.ServicioMiniDTO;
import com.gg.turnlook.features.servicio.dto.ServicioModificarDTO;
import com.gg.turnlook.features.servicio.dto.ServicioResponseDTO;
import com.gg.turnlook.features.sucursal.Sucursal;
import com.gg.turnlook.features.sucursal.SucursalService;
import com.gg.turnlook.features.usuario.Usuario;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;



@Service
public class ServicioService {



    private final ServicioRepository servRepo;
    private final SucursalService sucService;
    private final ImagenService imagenService;
    private final ReseniaService reseniaService;



    public ServicioService(ServicioRepository servRepo, SucursalService sucService, ImagenService imagenService, ReseniaService reseniaService) {
        this.servRepo = servRepo;
        this.sucService = sucService;
        this.imagenService = imagenService;
        this.reseniaService = reseniaService;
    }



    /// METODOS



    public void crearServicio(ServicioCrearDTO servicio, String userEmail) {

        Sucursal sucursal = sucService.listarSucursalPorId(servicio.getSucursalId());

        if(!sucService.enSucursal(userEmail, sucursal)){
            throw new ForbiddenException("No tenes permisos");
        }

        if(servicio.getDuracion() % 30 != 0){
            throw new BadRequestException("La duracion tiene que ser en intervalos de 30 minutos");
        }

        Servicio serv = new Servicio(
                servicio.getNombre(), servicio.getDescripcion(), servicio.getDuracion(),
                servicio.getPrecio(), sucursal
        );

        serv.setFotoPerfil(imagenService.crearFotoPerfil(servicio.getFotoUrl(), servicio.getFotoPublicId()));

        servRepo.save(serv);
    }


    public void modificarServicio(ServicioModificarDTO servicio, Integer servicioId,
                                  String userEmail) {

        Servicio servOld = listarServicioPorId(servicioId);

        if(!sucService.enSucursal(userEmail, servOld.getSucursal())){
            throw new ForbiddenException("No tenes permisos");
        }

        if (servicio.getNombre() != null) servOld.setNombre(servicio.getNombre());

        if (servicio.getDescripcion() != null) servOld.setDescripcion(servicio.getDescripcion());

        if (servicio.getDuracion() != null) servOld.setDuracion(servicio.getDuracion());

        if (servicio.getPrecio() != null) servOld.setPrecio(servicio.getPrecio());

        if(servOld.getDuracion() % 30 != 0){
            throw new BadRequestException("La duracion tiene que ser en intervalos de 30 minutos");
        }

        if(servicio.getFotoUrl() != null && !servicio.getFotoUrl().isBlank()){
            imagenService.cambiarFotoPerfilServicio(servOld, servicio.getFotoUrl(), servicio.getFotoPublicId());
        }

        servRepo.save(servOld);
    }


    public void eliminarServicio(Integer servicioId, String empleadorEmail) {

        Servicio servicio = listarServicioPorId(servicioId);

        if(!servicio.getSucursal().getEmpleador().getEmail().equals(empleadorEmail)){
            throw new ForbiddenException("No tenes permisos");
        }

        servicio.setActivo(false);

        servRepo.save(servicio);
    }


    // ESTE seria para admin dsp ver (sacar seguramente)
    public List<Servicio> listarServicios() {
        return servRepo.findByActivoTrue();
    }


    public List<ServicioMiniDTO> listarServiciosSucursal(Integer sucursalId){

        sucService.listarSucursalPorId(sucursalId);  //validacion

        List<Servicio> servicios = servRepo.findBySucursalId(sucursalId);

        // Las puntuaciones de todos los servicios en una sola query, en vez de
        // dos por servicio.
        Map<Integer, ResumenPuntuacion> puntuaciones =
                reseniaService.getPuntuacionesServicios(
                        servicios.stream().map(Servicio::getId).toList());

        return servicios.stream()
                .map(s -> new ServicioMiniDTO(s.getId(), s.getNombre(),
                        s.getDuracion(), s.getPrecio(), s.getDescripcion(),
                        s.getFotoPerfil().getFotoValida(),
                        puntuacionDe(puntuaciones.get(s.getId())),
                        cantidadDe(puntuaciones.get(s.getId())))
                )
                .toList();
    }


    private static Double puntuacionDe(ResumenPuntuacion resumen) {

        return resumen == null ? null : resumen.promedioConUnDecimal();
    }


    private static Long cantidadDe(ResumenPuntuacion resumen) {

        return resumen == null ? 0L : resumen.cantidad();
    }


    public ServicioResponseDTO verServicioPorId(Integer idServicio) {
        // Consulta anterior a la optimización. Reemplazada por findByIdConDetalle.
        // return mapearServicio(listarServicioPorId(idServicio));
        Servicio servicio = servRepo.findByIdConDetalle(idServicio)
                .orElseThrow(() -> new NotFoundException("Servicio no encontrado"));
        return mapearServicio(servicio);
    }


    private ServicioResponseDTO mapearServicio(Servicio s) {
        ServicioResponseDTO dto = new ServicioResponseDTO();

        dto.setId(s.getId());

        dto.setNombre(s.getNombre());

        dto.setDescripcion(s.getDescripcion());

        dto.setDuracion(s.getDuracion());

        dto.setPrecio(s.getPrecio());

        dto.setFotoPerfil(s.getFotoPerfil().getFotoValida());

        // Consulta anterior a la optimización. Reemplazada por getPuntuacionesServicios.
        // dto.setPuntuacion(reseniaService.getPuntuacionPromedioServicio(s.getId()));
        // dto.setCantidadPuntuaciones(reseniaService.getPuntuacionesTotalesServicio(s.getId()));
        Map<Integer, ResumenPuntuacion> puntuaciones =
                reseniaService.getPuntuacionesServicios(List.of(s.getId()));
        dto.setPuntuacion(puntuacionDe(puntuaciones.get(s.getId())));
        dto.setCantidadPuntuaciones(cantidadDe(puntuaciones.get(s.getId())));

        dto.setNombreSucursal(s.getSucursal().getNombre());

        dto.setDireccionSucursal(s.getSucursal().getDireccion());

        dto.setCategoriaSucursal(s.getSucursal().getCategoria().getCategoria().name());

        return dto;
    }


    public Servicio listarServicioPorId(Integer servicioId) {
        return servRepo.findById(servicioId).
                orElseThrow(() -> new NotFoundException("Servicio no encontrado"));
    }


}




