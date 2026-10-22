package mx.gestorsalon.controller;

import mx.gestorsalon.config.SecurityConfig;
import mx.gestorsalon.dto.equipo.ActualizarTrabajadorRequest;
import mx.gestorsalon.dto.equipo.RolNegocioResponse;
import mx.gestorsalon.dto.equipo.TrabajadorRequest;
import mx.gestorsalon.dto.equipo.TrabajadorResponse;
import mx.gestorsalon.exception.AccesoDenegadoException;
import mx.gestorsalon.exception.RecursoDuplicadoException;
import mx.gestorsalon.exception.RecursoNoEncontradoException;
import mx.gestorsalon.model.Negocio;
import mx.gestorsalon.model.RolNegocio;
import mx.gestorsalon.model.Usuario;
import mx.gestorsalon.model.enums.Permiso;
import mx.gestorsalon.model.enums.Rol;
import mx.gestorsalon.security.CustomUserDetails;
import mx.gestorsalon.security.CustomUserDetailsService;
import mx.gestorsalon.security.JwtService;
import mx.gestorsalon.service.EquipoService;
import mx.gestorsalon.service.RolNegocioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Capa web del equipo de trabajo (/equipo) y de los roles del negocio
 * (/roles-negocio), ambos con @PreAuthorize sobre permisos PERM_*.
 *
 * Estos controladores leen el negocio del @AuthenticationPrincipal, así que la
 * sesión se arma con un CustomUserDetails REAL envolviendo un Usuario: las
 * authorities salen de CustomUserDetails.getAuthorities() (ADMIN = todo el
 * catálogo, EMPLEADO = solo lo de su RolNegocio), igual que en producción.
 */
@WebMvcTest({EquipoController.class, RolNegocioController.class})
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class EquipoControllerTest {

    private static final String SIN_PERMISO = "No tienes permiso para realizar esta acción";

    private static final String TRABAJADOR = """
            {"nombre":"Luis Gómez","correo":"luis@laspalmas.mx","password":"Temporal123","rolNegocioId":3}""";
    private static final String ACTUALIZACION = """
            {"nombre":"Luis Gómez","rolNegocioId":4}""";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EquipoService equipoService;
    @MockitoBean
    private RolNegocioService rolNegocioService;
    // JwtAuthenticationFilter es un @Component que @WebMvcTest sí carga: sus dependencias van como mocks.
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private Negocio laspalmas;
    private CustomUserDetails dueno;

    @BeforeEach
    void setUp() {
        laspalmas = new Negocio();
        laspalmas.setId(7L);
        laspalmas.setNombre("Salón Las Palmas");
        dueno = new CustomUserDetails(usuario(1L, "ana@laspalmas.mx", Rol.ADMIN, null));
    }

    // -------------------- Ayudantes --------------------

    private Usuario usuario(Long id, String correo, Rol rol, RolNegocio rolNegocio) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre(correo);
        usuario.setCorreo(correo);
        usuario.setPasswordHash("hash-bcrypt");
        usuario.setRol(rol);
        usuario.setRolNegocio(rolNegocio);
        usuario.setNegocio(laspalmas);
        usuario.setActivo(true);
        return usuario;
    }

    private static RolNegocio rolNegocio(Long id, String nombre, Set<Permiso> permisos) {
        RolNegocio rol = new RolNegocio();
        rol.setId(id);
        rol.setNombre(nombre);
        rol.setDescripcion("Puesto " + nombre);
        rol.setPermisos(permisos);
        return rol;
    }

    /** Un EMPLEADO del mismo negocio cuyo RolNegocio concede exactamente estos permisos. */
    private CustomUserDetails empleadoCon(Permiso... permisos) {
        Set<Permiso> concedidos = EnumSet.noneOf(Permiso.class);
        Collections.addAll(concedidos, permisos);
        return new CustomUserDetails(usuario(2L, "luis@laspalmas.mx", Rol.EMPLEADO,
                rolNegocio(3L, "Recepcionista", concedidos)));
    }

    private static MockHttpServletRequestBuilder peticion(String metodo, String url, String cuerpo) {
        MockHttpServletRequestBuilder builder = request(HttpMethod.valueOf(metodo), url);
        return cuerpo == null ? builder : builder.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    private static TrabajadorResponse trabajador(Long id, boolean activo) {
        return TrabajadorResponse.builder()
                .id(id)
                .nombre("Luis Gómez")
                .correo("luis@laspalmas.mx")
                .activo(activo)
                .rol(RolNegocioResponse.builder().id(3L).nombre("Recepcionista").esSistema(true).build())
                .build();
    }

    // -------------------- /equipo como ADMIN --------------------

    @Test
    @DisplayName("GET /equipo lista el equipo del negocio del usuario autenticado")
    void listarEquipo_admin_usaNegocioDelPrincipal() throws Exception {
        // Arrange
        when(equipoService.listarEquipo(7L)).thenReturn(List.of(trabajador(2L, true)));

        // Act
        ResultActions resultado = mvc.perform(get("/equipo").with(user(dueno)));

        // Assert
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].correo").value("luis@laspalmas.mx"))
                .andExpect(jsonPath("$[0].rol.nombre").value("Recepcionista"));
    }

    @Test
    @DisplayName("POST /equipo entrega al servicio el actor autenticado (para el techo de privilegio) y la solicitud")
    void registrarTrabajador_admin_delegaConElActor() throws Exception {
        // Arrange
        when(equipoService.registrarTrabajador(same(dueno), any(TrabajadorRequest.class)))
                .thenReturn(trabajador(2L, true));

        // Act
        ResultActions resultado = mvc.perform(post("/equipo").with(user(dueno))
                .contentType(MediaType.APPLICATION_JSON).content(TRABAJADOR));

        // Assert
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.password").doesNotExist());
        verify(equipoService).registrarTrabajador(same(dueno), argThat(req ->
                "luis@laspalmas.mx".equals(req.getCorreo()) && Long.valueOf(3L).equals(req.getRolNegocioId())));
    }

    @Test
    @DisplayName("PUT /equipo/{id} pasa al servicio el id de la ruta, el actor y la solicitud")
    void actualizarTrabajador_admin_pasaIdDeLaRuta() throws Exception {
        // Arrange
        when(equipoService.actualizarTrabajador(eq(2L), same(dueno), any(ActualizarTrabajadorRequest.class)))
                .thenReturn(trabajador(2L, true));

        // Act
        ResultActions resultado = mvc.perform(put("/equipo/2").with(user(dueno))
                .contentType(MediaType.APPLICATION_JSON).content(ACTUALIZACION));

        // Assert
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
        verify(equipoService).actualizarTrabajador(eq(2L), same(dueno),
                argThat(req -> Long.valueOf(4L).equals(req.getRolNegocioId())));
    }

    @ParameterizedTest(name = "activo={0}")
    @ValueSource(booleans = {true, false})
    @DisplayName("PATCH /equipo/{id}/estado traduce el parámetro activo y lo entrega al servicio")
    void cambiarEstado_admin_pasaElEstadoPedido(boolean activo) throws Exception {
        // Arrange
        when(equipoService.cambiarEstado(2L, dueno, activo)).thenReturn(trabajador(2L, activo));

        // Act
        ResultActions resultado = mvc.perform(patch("/equipo/2/estado")
                .param("activo", String.valueOf(activo)).with(user(dueno)));

        // Assert
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(activo));
        verify(equipoService).cambiarEstado(eq(2L), same(dueno), eq(activo));
    }

    // -------------------- /equipo: permisos --------------------

    static Stream<Arguments> rutasDeEquipo() {
        return Stream.of(
                arguments("GET", "/equipo", null),
                arguments("POST", "/equipo", TRABAJADOR),
                arguments("PUT", "/equipo/5", ACTUALIZACION),
                arguments("PATCH", "/equipo/5/estado?activo=false", null));
    }

    @ParameterizedTest(name = "{0} {1} como EMPLEADO sin permisos de equipo -> 403")
    @MethodSource("rutasDeEquipo")
    @DisplayName("Un EMPLEADO sin EQUIPO_VER ni EQUIPO_GESTIONAR recibe 403 y el servicio no se ejecuta")
    void equipo_empleadoSinPermisos_responde403(String metodo, String url, String cuerpo) throws Exception {
        // Arrange
        CustomUserDetails recepcionista = empleadoCon(Permiso.CLIENTE_VER, Permiso.TURNO_CREAR);

        // Act
        ResultActions resultado = mvc.perform(peticion(metodo, url, cuerpo).with(user(recepcionista)));

        // Assert
        resultado.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(SIN_PERMISO));
        verifyNoInteractions(equipoService);
    }

    @ParameterizedTest(name = "{0} {1} solo con EQUIPO_VER -> {3}")
    @CsvSource(delimiter = '|', nullValues = "NULL", textBlock = """
            GET   | /equipo                      | NULL                                                                                          | 200
            POST  | /equipo                      | {"nombre":"Luis Gómez","correo":"luis@laspalmas.mx","password":"Temporal123","rolNegocioId":3} | 403
            PUT   | /equipo/5                    | {"nombre":"Luis Gómez","rolNegocioId":4}                                                      | 403
            PATCH | /equipo/5/estado?activo=true | NULL                                                                                          | 403
            """)
    @DisplayName("EQUIPO_VER permite consultar el equipo pero no modificarlo")
    void equipo_empleadoSoloConVer_puedeListarPeroNoGestionar(String metodo, String url, String cuerpo,
            int estadoEsperado) throws Exception {
        // Arrange
        CustomUserDetails coordinador = empleadoCon(Permiso.EQUIPO_VER);

        // Act
        ResultActions resultado = mvc.perform(peticion(metodo, url, cuerpo).with(user(coordinador)));

        // Assert
        resultado.andExpect(status().is(estadoEsperado));
    }

    @ParameterizedTest(name = "{0} {1} sin token -> 401")
    @CsvSource({"GET, /equipo", "POST, /equipo", "PATCH, /equipo/5/estado", "GET, /roles-negocio"})
    @DisplayName("Sin token, /equipo y /roles-negocio responden 401")
    void equipo_sinToken_responde401(String metodo, String url) throws Exception {
        // Arrange
        MockHttpServletRequestBuilder anonima = request(HttpMethod.valueOf(metodo), url);

        // Act
        ResultActions resultado = mvc.perform(anonima);

        // Assert
        resultado.andExpect(status().isUnauthorized());
        verifyNoInteractions(equipoService, rolNegocioService);
    }

    // -------------------- /equipo: validación y errores del servicio --------------------

    @ParameterizedTest(name = "{0} {1} -> \"{3}\"")
    @CsvSource(delimiter = '|', textBlock = """
            POST | /equipo   | {"nombre":"Luis","correo":"luis-sin-arroba","password":"Temporal123","rolNegocioId":3} | El formato de correo no es válido
            POST | /equipo   | {"nombre":"Luis","correo":"luis@laspalmas.mx","password":"  ","rolNegocioId":3}       | La contraseña es obligatoria
            POST | /equipo   | {"nombre":"Luis","correo":"luis@laspalmas.mx","password":"Temporal123"}              | El rol es obligatorio
            PUT  | /equipo/5 | {"nombre":"","rolNegocioId":4}                                                        | El nombre es obligatorio
            """)
    @DisplayName("Una solicitud de trabajador inválida responde 400 con el mensaje de la validación")
    void equipo_solicitudInvalida_responde400(String metodo, String url, String cuerpo, String mensaje)
            throws Exception {
        // Arrange
        RequestPostProcessor sesion = user(dueno);

        // Act
        ResultActions resultado = mvc.perform(peticion(metodo, url, cuerpo).with(sesion));

        // Assert
        resultado.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(mensaje));
        verifyNoInteractions(equipoService);
    }

    static Stream<Arguments> erroresDelServicio() {
        return Stream.of(
                arguments(new AccesoDenegadoException("No puedes conceder permisos que no tienes: ROL_GESTIONAR"),
                        403, "No puedes conceder permisos que no tienes: ROL_GESTIONAR"),
                arguments(new AccesoDenegadoException("No puedes cambiar tu propio estado."),
                        403, "No puedes cambiar tu propio estado."),
                arguments(new RecursoNoEncontradoException("Trabajador no encontrado"),
                        404, "Trabajador no encontrado"),
                arguments(new IllegalArgumentException(
                                "Solo se puede gestionar usuarios con rol EMPLEADO a través de este módulo"),
                        400, "Solo se puede gestionar usuarios con rol EMPLEADO a través de este módulo"));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("erroresDelServicio")
    @DisplayName("Las reglas del servicio de equipo llegan al cliente con su código y su mensaje")
    void cambiarEstado_servicioLanzaExcepcion_respondeCodigoDelHandler(RuntimeException excepcion, int estado,
            String mensaje) throws Exception {
        // Arrange
        when(equipoService.cambiarEstado(anyLong(), any(CustomUserDetails.class), anyBoolean())).thenThrow(excepcion);
        RequestPostProcessor sesion = user(dueno);

        // Act
        ResultActions resultado = mvc.perform(patch("/equipo/1/estado").param("activo", "false").with(sesion));

        // Assert
        resultado.andExpect(status().is(estado))
                .andExpect(jsonPath("$.message").value(mensaje));
    }

    @Test
    @DisplayName("POST /equipo con un correo ya registrado responde 409 con un mensaje opaco")
    void registrarTrabajador_correoDuplicado_responde409() throws Exception {
        // Arrange
        when(equipoService.registrarTrabajador(any(CustomUserDetails.class), any(TrabajadorRequest.class)))
                .thenThrow(new RecursoDuplicadoException("El correo ya se encuentra registrado o no es válido."));
        RequestPostProcessor sesion = user(dueno);

        // Act
        ResultActions resultado = mvc.perform(post("/equipo").with(sesion)
                .contentType(MediaType.APPLICATION_JSON).content(TRABAJADOR));

        // Assert
        resultado.andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El correo ya se encuentra registrado o no es válido."));
    }

    // -------------------- /roles-negocio --------------------

    @Test
    @DisplayName("GET /roles-negocio devuelve los roles del negocio de la sesión sin exponer sus permisos")
    void listarRoles_admin_devuelveRolesDelNegocioSinPermisos() throws Exception {
        // Arrange
        RolNegocio gerente = rolNegocio(3L, "Gerente", EnumSet.allOf(Permiso.class));
        gerente.setEsSistema(true);
        gerente.setNegocio(laspalmas);
        RolNegocio valet = rolNegocio(9L, "Valet", EnumSet.of(Permiso.TURNO_VER));
        when(rolNegocioService.obtenerRolesPorNegocio(7L)).thenReturn(List.of(gerente, valet));
        RequestPostProcessor sesion = user(dueno);

        // Act
        ResultActions resultado = mvc.perform(get("/roles-negocio").with(sesion));

        // Assert
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].nombre").value("Gerente"))
                .andExpect(jsonPath("$[0].descripcion").value("Puesto Gerente"))
                .andExpect(jsonPath("$[0].esSistema").value(true))
                .andExpect(jsonPath("$[0].permisos").doesNotExist())
                .andExpect(jsonPath("$[0].negocio").doesNotExist())
                .andExpect(jsonPath("$[1].esSistema").value(false));
    }

    static Stream<Arguments> permisosQueDejanVerRoles() {
        return Stream.of(
                arguments(EnumSet.of(Permiso.ROL_VER)),
                arguments(EnumSet.of(Permiso.EQUIPO_GESTIONAR)));
    }

    @ParameterizedTest(name = "EMPLEADO con {0} -> 200")
    @MethodSource("permisosQueDejanVerRoles")
    @DisplayName("Para ver los roles basta ROL_VER o EQUIPO_GESTIONAR (quien gestiona el equipo debe poder asignarlos)")
    void listarRoles_empleadoConRolVerOEquipoGestionar_responde200(Set<Permiso> permisos) throws Exception {
        // Arrange
        CustomUserDetails empleado = empleadoCon(permisos.toArray(new Permiso[0]));

        // Act
        ResultActions resultado = mvc.perform(get("/roles-negocio").with(user(empleado)));

        // Assert
        resultado.andExpect(status().isOk());
        verify(rolNegocioService).obtenerRolesPorNegocio(7L);
    }

    static Stream<Arguments> permisosQueNoDejanVerRoles() {
        return Stream.of(
                arguments(EnumSet.of(Permiso.EQUIPO_VER, Permiso.ROL_GESTIONAR)),
                arguments(EnumSet.noneOf(Permiso.class)));
    }

    @ParameterizedTest(name = "EMPLEADO con {0} -> 403")
    @MethodSource("permisosQueNoDejanVerRoles")
    @DisplayName("Sin ROL_VER ni EQUIPO_GESTIONAR, ver los roles responde 403")
    void listarRoles_empleadoSinPermisoDeRoles_responde403(Set<Permiso> permisos) throws Exception {
        // Arrange
        CustomUserDetails empleado = empleadoCon(permisos.toArray(new Permiso[0]));

        // Act
        ResultActions resultado = mvc.perform(get("/roles-negocio").with(user(empleado)));

        // Assert
        resultado.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(SIN_PERMISO));
        verifyNoInteractions(rolNegocioService);
    }
}