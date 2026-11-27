package com.gg.turnlook.features.paginacion;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gg.turnlook.common.dto.PaginaDTO;
import com.gg.turnlook.common.exception.BadRequestException;
import com.gg.turnlook.common.exception.GlobalExceptionHandler;
import com.gg.turnlook.features.resenia.ReseniaService;
import com.gg.turnlook.features.sucursal.SucursalController;
import com.gg.turnlook.features.sucursal.SucursalDescubrimientoService;
import com.gg.turnlook.features.sucursal.SucursalService;
import com.gg.turnlook.features.sucursal.dto.PaginaSucursalesDTO;
import com.gg.turnlook.features.usuario.UsuarioController;
import com.gg.turnlook.features.usuario.UsuarioService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Verifica que los controladores reenvian los filtros opcionales al servicio. */
class AdminListadosFiltrosHttpTest {

    @Test
    void usuariosPaginadosRecibenTodosLosFiltros() throws Exception {
        UsuarioService service = mock(UsuarioService.class);
        when(service.listarUsuariosPaginados(
                "Ana", "Gomez", true, "ana@", "cliente", "2"))
                .thenReturn(new PaginaDTO<>(List.of(), 0, 0, 2, 10));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UsuarioController(service)).build();

        mockMvc.perform(get("/usuarios/listar/paginado")
                        .param("nombre", "Ana")
                        .param("apellido", "Gomez")
                        .param("activo", "true")
                        .param("email", "ana@")
                        .param("rol", "cliente")
                        .param("page", "2"))
                .andExpect(status().isOk());

        verify(service).listarUsuariosPaginados(
                "Ana", "Gomez", true, "ana@", "cliente", "2");
    }

    @Test
    void usuariosFiltradosRecibenEmailYRol() throws Exception {
        UsuarioService service = mock(UsuarioService.class);
        when(service.filtrarListaUsuarios("Ana", null, null, "@example.com", "EMPLEADO"))
                .thenReturn(List.of());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UsuarioController(service)).build();

        mockMvc.perform(get("/usuarios/listar/filtrar")
                        .param("nombre", "Ana")
                        .param("email", "@example.com")
                        .param("rol", "EMPLEADO"))
                .andExpect(status().isOk());

        verify(service).filtrarListaUsuarios("Ana", null, null, "@example.com", "EMPLEADO");
    }

    @Test
    void rolInvalidoDevuelveErrorDelPedido() throws Exception {
        UsuarioService service = mock(UsuarioService.class);
        when(service.listarUsuariosPaginados(null, null, null, null, "INVALIDO", "0"))
                .thenThrow(new BadRequestException("Rol inválido"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UsuarioController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/usuarios/listar/paginado").param("rol", "INVALIDO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sucursalesAdminRecibenBusquedaLibreNombreYCategoria() throws Exception {
        SucursalDescubrimientoService descubrimiento = mock(SucursalDescubrimientoService.class);
        when(descubrimiento.listarPaginadoAdmin("spa", "Central", "SPA", "1"))
                .thenReturn(new PaginaSucursalesDTO(List.of(), 0, 0, 1, 10));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new SucursalController(
                mock(SucursalService.class), descubrimiento, mock(ReseniaService.class))).build();

        mockMvc.perform(get("/sucursales/listar/paginado/admin")
                        .param("q", "spa")
                        .param("nombre", "Central")
                        .param("categoria", "SPA")
                        .param("page", "1"))
                .andExpect(status().isOk());

        verify(descubrimiento).listarPaginadoAdmin("spa", "Central", "SPA", "1");
    }
}
