package com.gg.turnlook.Backend.Service;



import com.gg.turnlook.Backend.Enum.EstadoSolicitud;
import com.gg.turnlook.Backend.Excepciones.ConflictException;
import com.gg.turnlook.Backend.Model.SolicitudEmpleado;
import com.gg.turnlook.Backend.Model.Sucursal;
import com.gg.turnlook.Backend.Model.Usuario;
import com.gg.turnlook.Backend.Repository.SolicitudEmpleadoRepository;
import org.springframework.stereotype.Service;


/// ESTA CLASE
///  es para q no haya dependencia circular entre servicioService y solicitudEmpleadoService

@Service
public class GestionEmpleadoService {


    private final SolicitudEmpleadoRepository solicitudEmpleadoRepo;



    public GestionEmpleadoService(SolicitudEmpleadoRepository solicitudRepo) {
        this.solicitudEmpleadoRepo = solicitudRepo;
    }



    /// METODO


    public void enviarSolicitud(Usuario empleado, Sucursal sucursal){

        if (solicitudEmpleadoRepo.existsByEmpleadoIdAndSucursalIdAndEstado(
                empleado.getId(), sucursal.getId(), EstadoSolicitud.PENDIENTE)) {
            throw new ConflictException(
                    "El usuario ya tiene una solicitud pendiente de esta sucursal");
        }

        SolicitudEmpleado solicitud = new SolicitudEmpleado(sucursal, empleado);

        solicitudEmpleadoRepo.save(solicitud);
    }
}
