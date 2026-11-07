package com.gg.turnlook.Backend.Controllers;


import com.gg.turnlook.Backend.DTO.Imagen.ImagenDTO;
import com.gg.turnlook.Backend.DTO.Sucursal.SucursalCrearDTO;
import com.gg.turnlook.Backend.DTO.Sucursal.SucursalModificarDTO;
import com.gg.turnlook.Backend.DTO.Usuario.UsuarioEmailDTO;
import com.gg.turnlook.Backend.Enum.ECategoria;
import com.gg.turnlook.Backend.Service.ReseniaService;
import com.gg.turnlook.Backend.Service.SucursalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/sucursales")
@CrossOrigin(origins = "*")
public class SucursalController {


    private final SucursalService sucursalService;
    private final ReseniaService reseniaService;


    public SucursalController(SucursalService sucursalService, ReseniaService reseniaService) {
        this.sucursalService = sucursalService;
        this.reseniaService = reseniaService;
    }


    /// ENDPOINTS


    @PreAuthorize("hasRole('EMPLEADOR')")
    @PostMapping("/crear")
    public ResponseEntity<?> crearSucursal(
            @Valid @RequestBody SucursalCrearDTO sucursal,
            @AuthenticationPrincipal String empleadorEmail) {

        sucursalService.crearSucursal(sucursal, empleadorEmail);
        return ResponseEntity.ok().body("Se creó la sucursal");
    }


    // ver si meto ADMIN , seguro NO
    @PreAuthorize("hasRole('EMPLEADOR')")
    @PatchMapping("/modificar/{sucursalId}")
    public ResponseEntity<?> modificarSucursal(@PathVariable("sucursalId") Integer sucursalId,
                                               @Valid @RequestBody SucursalModificarDTO sucursal,
                                               @AuthenticationPrincipal String empleadorEmail) {

        sucursalService.modificarSucursal(sucursal, sucursalId, empleadorEmail);
        return ResponseEntity.ok().body("Se modificó la sucursal");
    }


    // ver si admin tmb
    @PreAuthorize("hasRole('EMPLEADOR')")
    @GetMapping("/{sucursalId}/imagenes")
    public ResponseEntity<?> verImagenesPorSucursal(
            @PathVariable("sucursalId") Integer sucursalId,
            @AuthenticationPrincipal String empleadorEmail) {

        return ResponseEntity.ok().body(sucursalService.listarImagenesPorSucursal(
                sucursalId, empleadorEmail));
    }


    @PreAuthorize("hasRole('EMPLEADOR')")
    @PostMapping("/{sucursalId}/agregar/imagenes")
    public ResponseEntity<?> agregarImagenes(@PathVariable("sucursalId") Integer sucursalId,
                                             @Valid @RequestBody ImagenDTO imagenes,
                                             @AuthenticationPrincipal String empleadorEmail) {

        sucursalService.agregarImagenes(imagenes, sucursalId, empleadorEmail);
        return ResponseEntity.ok().body("Se agregaron las imagenes a la sucursal");
    }


    // ver si admin tmb
    @PreAuthorize("hasRole('EMPLEADOR')")
    @DeleteMapping("/{sucursalId}/eliminar/imagen/{imagenId}")
    public ResponseEntity<?> eliminarImagen(@PathVariable("sucursalId") Integer sucursalId,
                                            @PathVariable("imagenId") Integer imagenId,
                                            @AuthenticationPrincipal String empleadorEmail) {

        sucursalService.eliminarImagen(imagenId, sucursalId, empleadorEmail);
        return ResponseEntity.ok().body("Se eliminó la imagen de la sucursal");
    }


    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @DeleteMapping("/eliminar/{sucursalId}")
    public ResponseEntity<?> eliminarSucursal(@PathVariable("sucursalId") Integer sucursalId) {

        sucursalService.eliminarSucursal(sucursalId);
        return ResponseEntity.ok().body("Se eliminó la sucursal");
    }


    @PreAuthorize("hasRole('EMPLEADOR')")
    @DeleteMapping("/borrar-sucursal-propia/{sucursalId}")
    public ResponseEntity<?> borrarSucursalPropia(
            @PathVariable("sucursalId") Integer sucursalId,
            @AuthenticationPrincipal String empleadorEmail) {

        sucursalService.borrarSucursalPropia(sucursalId, empleadorEmail);
        return ResponseEntity.ok().body("Se eliminó la sucursal");
    }


    @GetMapping("/listar")
    public ResponseEntity<?> listarSucursales() {

        return ResponseEntity.ok().body(sucursalService.listarSucursales());
    }


    @GetMapping("/listar/filtrar")
    public ResponseEntity<?> filtrarListaSucursales(
            @RequestParam(required = false) ECategoria categoria,
            @RequestParam(required = false) String nombre) {

        return ResponseEntity.ok().body(sucursalService.filtrarListaSucursales(categoria, nombre));
    }


    @PreAuthorize("hasRole('EMPLEADOR')")
    @GetMapping("/listar/propias")
    public ResponseEntity<?> listarSucursalesPropias(
            @AuthenticationPrincipal String empleadorEmail) {

        return ResponseEntity.ok().body(sucursalService.listarSucursalesPropias(empleadorEmail));
    }


    @GetMapping("/{sucursalId}")
    public ResponseEntity<?> sucursalPorId(@PathVariable("sucursalId") Integer sucursalId) {

        return ResponseEntity.ok().body(sucursalService.verSucursalPorId(sucursalId));
    }


    @PreAuthorize("hasAnyRole('EMPLEADOR','ADMINISTRADOR')")
    @GetMapping("/{sucursalId}/empleados")
    public ResponseEntity<?> empleadosPorSucursal(@PathVariable("sucursalId") Integer sucursalId,
                                                  @AuthenticationPrincipal String userEmail) {

        return ResponseEntity.ok().body(sucursalService.verEmpleados(sucursalId, userEmail));
    }


    @PreAuthorize("hasRole('EMPLEADOR')")
    @PostMapping("/{sucursalId}/empleados/agregar")
    public ResponseEntity<?> agregarEmpleado(@PathVariable("sucursalId") Integer sucursalId,
                                             @Valid @RequestBody UsuarioEmailDTO userEmail,
                                             @AuthenticationPrincipal String empleadorEmail) {

        sucursalService.agregarEmpleado(sucursalId, userEmail, empleadorEmail);
        return ResponseEntity.ok().body("Se envió una solicitud al usuario");
    }


    @PreAuthorize("hasRole('EMPLEADOR')")
    @DeleteMapping("/{sucursalId}/empleados/eliminar/{empleadoId}")
    public ResponseEntity<?> eliminarEmpleado(@PathVariable("sucursalId") Integer sucursalId,
                                              @PathVariable("empleadoId") Integer empleadoId,
                                              @AuthenticationPrincipal String empleadorEmail) {

        sucursalService.eliminarEmpleado(sucursalId, empleadoId, empleadorEmail);
        return ResponseEntity.ok().body("Se eliminó al empleado correctamente");
    }


    @PreAuthorize("hasRole('EMPLEADOR')")
    @PatchMapping("/{sucursalId}/convertirme-empleado")
    public ResponseEntity<?> convertirmeEnEmpleado(
            @PathVariable("sucursalId") Integer sucursalId,
            @AuthenticationPrincipal String empleadorEmail) {

        sucursalService.convertirmeEnEmpleado(sucursalId, empleadorEmail);
        return ResponseEntity.ok().body("Ahora sos empleado de la sucursal");
    }


    @PreAuthorize("hasRole('CLIENTE')")
    @GetMapping("/{sucursalId}/elegir-empleado")
    public ResponseEntity<?> elegirEmpleado(@PathVariable("sucursalId") Integer sucursalId) {

        return ResponseEntity.ok().body(sucursalService.verEmpleadosParaElegir(sucursalId));
    }


    @GetMapping("/categorias")
    public ResponseEntity<?> listarCategorias() {

        return ResponseEntity.ok().body(ECategoria.values());
    }


    @PreAuthorize("hasAnyRole('CLIENTE','EMPLEADO','EMPLEADOR','ADMINISTRADOR')")
    @GetMapping("/{sucursalId}/resenias")
    public ResponseEntity<?> verResenias(@PathVariable("sucursalId") Integer sucursalId) {

        return ResponseEntity.ok().body(reseniaService.getReseniasPorSucursal(sucursalId));
    }


}

