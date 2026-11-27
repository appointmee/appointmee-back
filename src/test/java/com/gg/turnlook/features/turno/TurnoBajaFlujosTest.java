package com.gg.turnlook.features.turno;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gg.turnlook.auth.BloqueoUsuario;
import com.gg.turnlook.auth.password.PasswordResetTokenRepository;
import com.gg.turnlook.auth.refresh.RefreshTokenRepository;
import com.gg.turnlook.features.imagen.ImagenRepository;
import com.gg.turnlook.features.imagen.ImagenService;
import com.gg.turnlook.features.resenia.ReseniaService;
import com.gg.turnlook.features.solicitud.empleado.GestionEmpleadoService;
import com.gg.turnlook.features.sucursal.Sucursal;
import com.gg.turnlook.features.sucursal.SucursalRepository;
import com.gg.turnlook.features.sucursal.SucursalService;
import com.gg.turnlook.features.sucursal.categoria.CategoriaRepository;
import com.gg.turnlook.features.usuario.rol.RolService;
import com.gg.turnlook.features.usuario.Usuario;
import com.gg.turnlook.features.usuario.UsuarioRepository;
import com.gg.turnlook.features.usuario.UsuarioService;
import com.gg.turnlook.features.verificacion.VerificacionRepository;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** SCRUM-365: los flujos de baja existentes disparan la cancelación de turnos. */
class TurnoBajaFlujosTest {

    @Test
    void bajaDeEmpleadoPorAdministradorCancelaSusTurnos() {
        TurnoBajaService bajas = mock(TurnoBajaService.class);
        UsuarioService service = servicioUsuarios(bajas);

        Usuario admin = usuario(1, "admin@example.com");
        Usuario empleado = usuario(9, "empleado@example.com");
        when(empleado.getCredencialesVersion()).thenReturn(0);
        when(usRepo().findByEmailAndActivoTrue("admin@example.com"))
                .thenReturn(Optional.of(admin));
        when(usRepo().findById(9)).thenReturn(Optional.of(empleado));
        when(bloqueo().bloquear(9)).thenReturn(empleado);

        service.eliminarUsuario(9, "admin@example.com");

        verify(bajas).cancelarPorBajaEmpleado(9);
    }

    @Test
    void bajaDeCuentaPropiaCancelaSusTurnos() {
        TurnoBajaService bajas = mock(TurnoBajaService.class);
        UsuarioService service = servicioUsuarios(bajas);

        Usuario empleado = usuario(9, "empleado@example.com");
        when(empleado.getCredencialesVersion()).thenReturn(0);
        when(usRepo().findByEmailAndActivoTrue("empleado@example.com"))
                .thenReturn(Optional.of(empleado));
        when(bloqueo().bloquear(9)).thenReturn(empleado);

        service.borrarCuentaPropia("empleado@example.com");

        verify(empleado).setActivo(false);
        verify(bajas).cancelarPorBajaEmpleado(9);
    }

    @Test
    void bajaDeSucursalCancelaSusTurnos() {
        TurnoBajaService bajas = mock(TurnoBajaService.class);
        SucursalRepository sucRepo = mock(SucursalRepository.class);
        SucursalService service = servicioSucursales(sucRepo, bajas);

        Sucursal sucursal = sucursal(sucRepo, 4);

        service.eliminarSucursal(4);

        verify(bajas).cancelarPorBajaSucursal(sucursal.getId());
    }

    @Test
    void borrarSucursalPropiaCancelaSusTurnos() {
        TurnoBajaService bajas = mock(TurnoBajaService.class);
        SucursalRepository sucRepo = mock(SucursalRepository.class);
        SucursalService service = servicioSucursales(sucRepo, bajas);

        Sucursal sucursal = sucursal(sucRepo, 4);

        service.borrarSucursalPropia(4, "empleador@example.com");

        verify(bajas).cancelarPorBajaSucursal(sucursal.getId());
    }

    @Test
    void desvinculacionCancelaSoloLosTurnosDeEsaSucursal() {
        TurnoBajaService bajas = mock(TurnoBajaService.class);
        SucursalRepository sucRepo = mock(SucursalRepository.class);
        UsuarioService usuarioService = mock(UsuarioService.class);
        SucursalService service = new SucursalService(sucRepo,
                mock(CategoriaRepository.class), mock(ImagenRepository.class),
                usuarioService, mock(ImagenService.class), mock(ReseniaService.class),
                mock(GestionEmpleadoService.class), mock(RolService.class), bajas);

        Sucursal sucursal = sucursal(sucRepo, 4);
        Usuario empleado = usuario(9, "empleado@example.com");
        Set<Usuario> empleados = new HashSet<>(Set.of(empleado));
        when(sucursal.getEmpleados()).thenReturn(empleados);
        when(usuarioService.listarUsuarioPorId(9)).thenReturn(empleado);

        service.eliminarEmpleado(4, 9, "empleador@example.com");

        verify(bajas).cancelarPorDesvinculacion(9, 4);
    }

    // Los mocks se guardan por test para poder prepararlos sin compartir estado.
    private UsuarioRepository usRepoMock;
    private BloqueoUsuario bloqueoMock;

    private UsuarioRepository usRepo() {
        return usRepoMock;
    }

    private BloqueoUsuario bloqueo() {
        return bloqueoMock;
    }

    private UsuarioService servicioUsuarios(TurnoBajaService bajas) {
        usRepoMock = mock(UsuarioRepository.class);
        bloqueoMock = mock(BloqueoUsuario.class);
        return new UsuarioService(usRepoMock, null, null, null, null,
                mock(RefreshTokenRepository.class), mock(PasswordResetTokenRepository.class),
                bloqueoMock, mock(VerificacionRepository.class), bajas);
    }

    private SucursalService servicioSucursales(SucursalRepository sucRepo,
                                               TurnoBajaService bajas) {
        return new SucursalService(sucRepo, mock(CategoriaRepository.class),
                mock(ImagenRepository.class), mock(UsuarioService.class),
                mock(ImagenService.class), mock(ReseniaService.class),
                mock(GestionEmpleadoService.class), mock(RolService.class), bajas);
    }

    private Sucursal sucursal(SucursalRepository sucRepo, Integer id) {
        Sucursal sucursal = mock(Sucursal.class);
        Usuario empleador = usuario(3, "empleador@example.com");
        when(sucursal.getId()).thenReturn(id);
        when(sucursal.getEmpleador()).thenReturn(empleador);
        when(sucRepo.findByIdConEmpleador(id)).thenReturn(Optional.of(sucursal));
        return sucursal;
    }

    private Usuario usuario(Integer id, String email) {
        Usuario usuario = mock(Usuario.class);
        when(usuario.getId()).thenReturn(id);
        when(usuario.getEmail()).thenReturn(email);
        return usuario;
    }
}
