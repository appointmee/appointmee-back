package com.gg.turnlook.features.sucursal;

import com.gg.turnlook.features.imagen.Imagen;
import com.gg.turnlook.features.imagen.ImagenRepository;
import com.gg.turnlook.features.imagen.ImagenService;
import com.gg.turnlook.features.resenia.ReseniaService;
import com.gg.turnlook.features.solicitud.empleado.GestionEmpleadoService;
import com.gg.turnlook.features.sucursal.categoria.CategoriaRepository;
import com.gg.turnlook.features.usuario.Usuario;
import com.gg.turnlook.features.usuario.UsuarioService;
import com.gg.turnlook.features.usuario.rol.RolService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SucursalServiceFotosEmpleadoTest {

    @Mock private SucursalRepository sucursalRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private ImagenRepository imagenRepository;
    @Mock private UsuarioService usuarioService;
    @Mock private ImagenService imagenService;
    @Mock private ReseniaService reseniaService;
    @Mock private GestionEmpleadoService gestionEmpleadoService;
    @Mock private RolService rolService;

    @InjectMocks private SucursalService sucursalService;

    @Test
    void devuelveLaFotoAsociadaAunqueElIdDeImagenSeaDistintoAlDelEmpleado() {
        Usuario empleado = new Usuario();
        empleado.setId(41);

        Imagen foto = new Imagen();
        foto.setId(9);
        foto.setUrl("https://res.cloudinary.com/appointmee/image/upload/perfiles/empleado.jpg");
        empleado.setFotoPerfil(foto);

        Sucursal sucursal = new Sucursal();
        sucursal.setId(7);
        sucursal.setEmpleados(Set.of(empleado));

        when(sucursalRepository.findByIdConEmpleador(7)).thenReturn(Optional.of(sucursal));
        when(imagenRepository.findAllById(List.of(9))).thenReturn(List.of(foto));
        when(reseniaService.getPuntuacionesEmpleados(List.of(41), 7)).thenReturn(Map.of());

        var resultado = sucursalService.verEmpleadosParaElegir(7);

        assertEquals(1, resultado.size());
        assertEquals(41, resultado.getFirst().getId());
        assertEquals(foto.getUrl(), resultado.getFirst().getUrlFotoPerfil());
    }
}
