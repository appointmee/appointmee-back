package com.gg.turnlook.Backend.Service;



import com.gg.turnlook.Backend.DTO.Servicio.ServicioCrearDTO;
import com.gg.turnlook.Backend.DTO.Servicio.ServicioMiniDTO;
import com.gg.turnlook.Backend.DTO.Servicio.ServicioModificarDTO;
import com.gg.turnlook.Backend.DTO.Servicio.ServicioResponseDTO;
import com.gg.turnlook.Backend.Excepciones.BadRequestException;
import com.gg.turnlook.Backend.Excepciones.ForbiddenException;
import com.gg.turnlook.Backend.Excepciones.NotFoundException;
import com.gg.turnlook.Backend.Model.Servicio;
import com.gg.turnlook.Backend.Model.Sucursal;
import com.gg.turnlook.Backend.Model.Usuario;
import com.gg.turnlook.Backend.Repository.ServicioRepository;
import org.springframework.stereotype.Service;

import java.util.List;



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

        serv.setFotoPerfil(imagenService.crearFotoPerfil(servicio.getFotoUrl()));

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
            imagenService.cambiarFotoPerfilServicio(servOld,  servicio.getFotoUrl());
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

        return servRepo.findBySucursalId(sucursalId).stream()
                .map(s -> new ServicioMiniDTO(s.getId(), s.getNombre(),
                        s.getDuracion(), s.getFotoPerfil().getFotoValida(),
                        reseniaService.getPuntuacionPromedioServicio(s.getId()),
                        reseniaService.getPuntuacionesTotalesServicio(s.getId()))
                )
                .toList();
    }


    public ServicioResponseDTO verServicioPorId(Integer idServicio) {
        return mapearServicio(listarServicioPorId(idServicio));
    }


    private ServicioResponseDTO mapearServicio(Servicio s) {
        ServicioResponseDTO dto = new ServicioResponseDTO();

        dto.setId(s.getId());

        dto.setNombre(s.getNombre());

        dto.setDescripcion(s.getDescripcion());

        dto.setDuracion(s.getDuracion());

        dto.setPrecio(s.getPrecio());

        dto.setFotoPerfil(s.getFotoPerfil().getFotoValida());

        dto.setPuntuacion(reseniaService.getPuntuacionPromedioServicio(s.getId()));

        dto.setCantidadPuntuaciones(reseniaService.getPuntuacionesTotalesServicio(s.getId()));

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




