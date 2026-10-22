package mx.gestorsalon.controller;

import mx.gestorsalon.config.SecurityConfig;
import mx.gestorsalon.dto.CategoriaDTO;
import mx.gestorsalon.dto.ClienteDTO;
import mx.gestorsalon.dto.PaqueteDTO;
import mx.gestorsalon.dto.ServicioDTO;
import mx.gestorsalon.dto.TurnoDTO;
import mx.gestorsalon.exception.AccesoDenegadoException;
import mx.gestorsalon.exception.RecursoNoEncontradoException;
import mx.gestorsalon.model.Negocio;
import mx.gestorsalon.model.enums.Permiso;
import mx.gestorsalon.model.enums.TipoCobro;
import mx.gestorsalon.security.CustomUserDetailsService;
import mx.gestorsalon.security.JwtService;
import mx.gestorsalon.service.CategoriaService;
import mx.gestorsalon.service.ClienteService;
import mx.gestorsalon.service.PaqueteService;
import mx.gestorsalon.service.ServicioService;
import mx.gestorsalon.service.TurnoService;
import mx.gestorsalon.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Capa web del catálogo del negocio: categorías, servicios, paquetes, clientes y
 * turnos.
 *
 * Se levanta solo la capa MVC con la SecurityConfig REAL (filtro JWT, reglas de
 * URL y @PreAuthorize); los servicios son mocks. Lo que se prueba es el contrato
 * HTTP: rutas y códigos, permisos, validación de DTOs, traducción de errores y
 * que el negocio (tenant) siempre sale de la sesión y nunca de la petición.
 */
@WebMvcTest({CategoriaController.class, ServicioController.class, PaqueteController.class,
        ClienteController.class, TurnoController.class})
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class CatalogoControllerTest {

    private static final String SIN_PERMISO = "No tienes permiso para realizar esta acción";
    private static final String CUERPO_INVALIDO = "La solicitud tiene un formato inválido o campos no reconocidos";
    private static final String ERROR_GENERICO = "Ocurrió un error inesperado. Inténtalo de nuevo más tarde.";

    private static final String CATEGORIA = """
            {"nombre":"Decoración","descripcion":"Arreglos florales"}""";
    private static final String SERVICIO = """
            {"nombre":"Mesa imperial","precio":1500.00,"tipoCobro":"POR_EVENTO","categoriaId":3}""";
    private static final String PAQUETE = """
            {"nombre":"Boda básica","precio":25000,"items":[{"servicioId":4,"cantidad":2}]}""";
    private static final String CLIENTE = """
            {"nombre":"Laura Pérez","telefono":"6871234567","email":"laura@correo.mx"}""";
    private static final String TURNO = """
            {"diaSemana":"SATURDAY","tipoTurno":"NOCHE","horaInicio":"18:00","horaFin":"23:30"}""";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CategoriaService categoriaService;
    @MockitoBean
    private ServicioService servicioService;
    @MockitoBean
    private PaqueteService paqueteService;
    @MockitoBean
    private ClienteService clienteService;
    @MockitoBean
    private TurnoService turnoService;
    @MockitoBean
    private SecurityUtils securityUtils;
    // JwtAuthenticationFilter es un @Component que @WebMvcTest sí carga: sus dependencias van como mocks.
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    /** El negocio que SecurityUtils resuelve a partir del token de la sesión. */
    private Negocio negocioDelToken;

    @BeforeEach
    void setUp() {
        negocioDelToken = new Negocio();
        negocioDelToken.setId(7L);
        negocioDelToken.setNombre("Salón Las Palmas");
        when(securityUtils.getNegocioActual()).thenReturn(negocioDelToken);
    }

    // -------------------- Ayudantes --------------------

    /** Lo que se espera que el controlador le haya pedido al servicio. */
    @FunctionalInterface
    interface Verificacion {
        void sobre(CatalogoControllerTest prueba);
    }

    private static Verificacion espera(Verificacion verificacion) {
        return verificacion;
    }

    private static MockHttpServletRequestBuilder peticion(String metodo, String url, String cuerpo) {
        MockHttpServletRequestBuilder builder = request(HttpMethod.valueOf(metodo), url);
        return cuerpo == null ? builder : builder.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    /** Un usuario autenticado que tiene exactamente estas authorities. */
    private static RequestPostProcessor usuarioCon(List<String> authorities) {
        List<GrantedAuthority> concedidas = authorities.stream()
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList();
        return user("empleado@laspalmas.mx").authorities(concedidas);
    }

    private static List<String> todosLosPermisos() {
        List<String> permisos = new ArrayList<>();
        for (Permiso permiso : Permiso.values()) {
            permisos.add("PERM_" + permiso.name());
        }
        return permisos;
    }

    /** El dueño del salón: ROLE_ADMIN + todo el catálogo de permisos (igual que CustomUserDetails). */
    private static RequestPostProcessor adminDelNegocio() {
        List<String> authorities = todosLosPermisos();
        authorities.add("ROLE_ADMIN");
        return usuarioCon(authorities);
    }

    /** Un empleado con TODOS los permisos menos uno: prueba que se exige el permiso exacto. */
    private static RequestPostProcessor empleadoConTodoMenos(String permisoExcluido) {
        List<String> authorities = todosLosPermisos();
        authorities.remove(permisoExcluido);
        authorities.add("ROLE_EMPLEADO");
        return usuarioCon(authorities);
    }

    // -------------------- Contrato de rutas --------------------

    static Stream<Arguments> rutas() {
        return Stream.of(
                arguments("GET", "/categorias", null, "PERM_CATEGORIA_VER", 200,
                        espera(t -> verify(t.categoriaService).getAllByNegocio(7L))),
                arguments("POST", "/categorias", CATEGORIA, "PERM_CATEGORIA_GESTIONAR", 201,
                        espera(t -> verify(t.categoriaService).create(same(t.negocioDelToken), any(CategoriaDTO.class)))),
                arguments("PUT", "/categorias/5", CATEGORIA, "PERM_CATEGORIA_GESTIONAR", 200,
                        espera(t -> verify(t.categoriaService).update(same(t.negocioDelToken), eq(5L), any(CategoriaDTO.class)))),
                arguments("DELETE", "/categorias/5", null, "PERM_CATEGORIA_GESTIONAR", 204,
                        espera(t -> verify(t.categoriaService).deactivate(same(t.negocioDelToken), eq(5L)))),

                arguments("GET", "/servicios", null, "PERM_SERVICIO_VER", 200,
                        espera(t -> verify(t.servicioService).getAllByNegocio(7L))),
                arguments("POST", "/servicios", SERVICIO, "PERM_SERVICIO_CREAR", 201,
                        espera(t -> verify(t.servicioService).create(same(t.negocioDelToken), any(ServicioDTO.class)))),
                arguments("PUT", "/servicios/5", SERVICIO, "PERM_SERVICIO_EDITAR", 200,
                        espera(t -> verify(t.servicioService).update(same(t.negocioDelToken), eq(5L), any(ServicioDTO.class)))),
                arguments("DELETE", "/servicios/5", null, "PERM_SERVICIO_ELIMINAR", 204,
                        espera(t -> verify(t.servicioService).deactivate(same(t.negocioDelToken), eq(5L)))),

                arguments("GET", "/paquetes", null, "PERM_PAQUETE_VER", 200,
                        espera(t -> verify(t.paqueteService).getAllByNegocio(7L))),
                arguments("POST", "/paquetes", PAQUETE, "PERM_PAQUETE_CREAR", 201,
                        espera(t -> verify(t.paqueteService).create(same(t.negocioDelToken), any(PaqueteDTO.class)))),
                arguments("PUT", "/paquetes/5", PAQUETE, "PERM_PAQUETE_EDITAR", 200,
                        espera(t -> verify(t.paqueteService).update(same(t.negocioDelToken), eq(5L), any(PaqueteDTO.class)))),
                arguments("DELETE", "/paquetes/5", null, "PERM_PAQUETE_ELIMINAR", 204,
                        espera(t -> verify(t.paqueteService).deactivate(same(t.negocioDelToken), eq(5L)))),

                arguments("GET", "/clientes", null, "PERM_CLIENTE_VER", 200,
                        espera(t -> verify(t.clienteService).getAllByNegocio(7L))),
                arguments("POST", "/clientes", CLIENTE, "PERM_CLIENTE_CREAR", 201,
                        espera(t -> verify(t.clienteService).create(same(t.negocioDelToken), any(ClienteDTO.class)))),
                arguments("PUT", "/clientes/5", CLIENTE, "PERM_CLIENTE_EDITAR", 200,
                        espera(t -> verify(t.clienteService).update(same(t.negocioDelToken), eq(5L), any(ClienteDTO.class)))),
                arguments("DELETE", "/clientes/5", null, "PERM_CLIENTE_ELIMINAR", 204,
                        espera(t -> verify(t.clienteService).deactivate(same(t.negocioDelToken), eq(5L)))),

                arguments("GET", "/turnos", null, "PERM_TURNO_VER", 200,
                        espera(t -> verify(t.turnoService).getAllByNegocio(7L))),
                arguments("POST", "/turnos", TURNO, "PERM_TURNO_CREAR", 201,
                        espera(t -> verify(t.turnoService).create(same(t.negocioDelToken), any(TurnoDTO.class)))),
                arguments("PUT", "/turnos/5", TURNO, "PERM_TURNO_EDITAR", 200,
                        espera(t -> verify(t.turnoService).update(same(t.negocioDelToken), eq(5L), any(TurnoDTO.class)))),
                arguments("DELETE", "/turnos/5", null, "PERM_TURNO_CANCELAR", 204,
                        espera(t -> verify(t.turnoService).delete(same(t.negocioDelToken), eq(5L)))));
    }

    @ParameterizedTest(name = "{0} {1} con {3} -> {4}")
    @MethodSource("rutas")
    @DisplayName("Cada endpoint responde su código y opera sobre el negocio del token y el id de la ruta")
    void ruta_conSuPermiso_respondeCodigoYUsaNegocioDelToken(String metodo, String url, String cuerpo,
            String permiso, int estadoEsperado, Verificacion llamadaEsperada) throws Exception {
        // Arrange
        RequestPostProcessor empleado = usuarioCon(List.of("ROLE_EMPLEADO", permiso));

        // Act
        ResultActions resultado = mvc.perform(peticion(metodo, url, cuerpo).with(empleado));

        // Assert
        resultado.andExpect(status().is(estadoEsperado));
        llamadaEsperada.sobre(this);
    }

    // -------------------- @PreAuthorize: falta el permiso --------------------

    static Stream<Arguments> rutasProtegidas() {
        return Stream.of(
                arguments("GET", "/categorias", null, "PERM_CATEGORIA_VER"),
                arguments("POST", "/categorias", CATEGORIA, "PERM_CATEGORIA_GESTIONAR"),
                arguments("PUT", "/categorias/5", CATEGORIA, "PERM_CATEGORIA_GESTIONAR"),
                arguments("DELETE", "/categorias/5", null, "PERM_CATEGORIA_GESTIONAR"),
                arguments("GET", "/servicios", null, "PERM_SERVICIO_VER"),
                arguments("POST", "/servicios", SERVICIO, "PERM_SERVICIO_CREAR"),
                arguments("PUT", "/servicios/5", SERVICIO, "PERM_SERVICIO_EDITAR"),
                arguments("DELETE", "/servicios/5", null, "PERM_SERVICIO_ELIMINAR"));
    }

    @ParameterizedTest(name = "{0} {1} con todos los permisos menos {3} -> 403")
    @MethodSource("rutasProtegidas")
    @DisplayName("Un empleado autenticado sin el permiso exacto recibe 403 y el servicio no se ejecuta")
    void ruta_sinPermisoRequerido_responde403(String metodo, String url, String cuerpo, String permisoRequerido)
            throws Exception {
        // Arrange
        RequestPostProcessor empleado = empleadoConTodoMenos(permisoRequerido);

        // Act
        ResultActions resultado = mvc.perform(peticion(metodo, url, cuerpo).with(empleado));

        // Assert
        resultado.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(SIN_PERMISO));
        verifyNoInteractions(categoriaService, servicioService, securityUtils);
    }

    @Test
    @DisplayName("El SUPERADMIN no tiene permisos de negocio: no puede ver el catálogo de un salón")
    void listarServicios_superadmin_responde403() throws Exception {
        // Arrange
        RequestPostProcessor superadmin = user("root@plataforma.mx").roles("SUPERADMIN");

        // Act
        ResultActions resultado = mvc.perform(get("/servicios").with(superadmin));

        // Assert
        resultado.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(SIN_PERMISO));
        verifyNoInteractions(servicioService);
    }

    @ParameterizedTest(name = "{0} {1} sin token -> 401")
    @CsvSource({"GET, /categorias", "POST, /servicios", "PUT, /paquetes/5", "DELETE, /clientes/5", "GET, /turnos"})
    @DisplayName("Sin token la petición se corta con 401 antes de llegar al controlador")
    void ruta_sinToken_responde401(String metodo, String url) throws Exception {
        // Arrange
        MockHttpServletRequestBuilder anonima = request(HttpMethod.valueOf(metodo), url);

        // Act
        ResultActions resultado = mvc.perform(anonima);

        // Assert
        resultado.andExpect(status().isUnauthorized());
        verifyNoInteractions(categoriaService, servicioService, paqueteService, clienteService, turnoService,
                securityUtils);
    }

    // -------------------- Validación de DTOs (@Valid) --------------------

    @ParameterizedTest(name = "{0} {1} {2} -> 400 \"{3}\"")
    @CsvSource(delimiter = '|', textBlock = """
            POST | /categorias   | {"nombre":"   "}                                                      | El nombre de la categoría es obligatorio
            PUT  | /categorias/5 | {"descripcion":"Sin nombre"}                                          | El nombre de la categoría es obligatorio
            POST | /servicios    | {"nombre":"Mesa","precio":0,"tipoCobro":"POR_EVENTO"}                | El precio debe ser mayor a 0
            PUT  | /servicios/5  | {"nombre":"Mesa","precio":100}                                        | El tipo de cobro es obligatorio
            POST | /paquetes     | {"nombre":"Boda","precio":100,"items":[]}                             | Un paquete debe incluir al menos un servicio
            POST | /paquetes     | {"nombre":"Boda","precio":100,"items":[{"servicioId":4,"cantidad":0}]} | La cantidad debe ser al menos 1
            POST | /clientes     | {"nombre":"Laura","telefono":"6871234567","email":"laura-sin-arroba"} | El formato del correo electrónico no es válido
            PUT  | /clientes/5   | {"nombre":"Laura","telefono":"  "}                                    | El teléfono del cliente es obligatorio
            POST | /turnos       | {"diaSemana":"SATURDAY","tipoTurno":"NOCHE","horaInicio":"18:00"}     | La hora de fin es requerida
            """)
    @DisplayName("Un DTO inválido responde 400 con el mensaje de la validación y no llega al servicio")
    void ruta_cuerpoInvalido_responde400ConMensajeDelDto(String metodo, String url, String cuerpo, String mensaje)
            throws Exception {
        // Arrange
        RequestPostProcessor admin = adminDelNegocio();

        // Act
        ResultActions resultado = mvc.perform(peticion(metodo, url, cuerpo).with(admin));

        // Assert
        resultado.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(mensaje));
        verifyNoInteractions(categoriaService, servicioService, paqueteService, clienteService, turnoService);
    }

    @Test
    @DisplayName("Si fallan varios campos, el 400 los reporta todos separados por \" | \"")
    void crearTurno_variosCamposInvalidos_reportaTodosLosMensajes() throws Exception {
        // Arrange
        RequestPostProcessor admin = adminDelNegocio();

        // Act
        ResultActions resultado = mvc.perform(post("/turnos").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{}"));

        // Assert
        resultado.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", allOf(
                        containsString("El día de la semana es requerido"),
                        containsString("El tipo de turno es requerido (MAÑANA, TARDE, NOCHE)"),
                        containsString("La hora de inicio es requerida"),
                        containsString("La hora de fin es requerida"),
                        containsString(" | "))));
        verifyNoInteractions(turnoService);
    }

    @ParameterizedTest(name = "{0} {1} {2} -> 400")
    @CsvSource(delimiter = '|', textBlock = """
            POST | /turnos     | {"diaSemana":"LUNES","tipoTurno":"NOCHE","horaInicio":"18:00","horaFin":"23:00"}
            PUT  | /turnos/5   | {"diaSemana":"SATURDAY","tipoTurno":"NOCHE","horaInicio":"6 pm","horaFin":"23:00"}
            POST | /servicios  | {"nombre":"Mesa","precio":100,"tipoCobro":"GRATIS"}
            PUT  | /clientes/5 | {"nombre":"Laura",
            """)
    @DisplayName("Un JSON malformado o con un enum/hora inválidos responde 400 con un mensaje fijo")
    void ruta_cuerpoIlegible_responde400ConMensajeFijo(String metodo, String url, String cuerpo) throws Exception {
        // Arrange
        RequestPostProcessor admin = adminDelNegocio();

        // Act
        ResultActions resultado = mvc.perform(peticion(metodo, url, cuerpo).with(admin));

        // Assert
        resultado.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(CUERPO_INVALIDO));
        verifyNoInteractions(servicioService, clienteService, turnoService);
    }

    // -------------------- Errores del servicio -> código HTTP --------------------

    static Stream<Arguments> erroresDelServicio() {
        return Stream.of(
                arguments(new RecursoNoEncontradoException("Servicio no encontrado"), 404, "Servicio no encontrado"),
                arguments(new AccesoDenegadoException("El servicio no pertenece a su negocio"), 403,
                        "El servicio no pertenece a su negocio"),
                arguments(new IllegalArgumentException("Ya existe un servicio activo con ese nombre"), 400,
                        "Ya existe un servicio activo con ese nombre"),
                arguments(new RuntimeException("could not execute statement; SQL [update servicios set ...]"), 500,
                        ERROR_GENERICO));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("erroresDelServicio")
    @DisplayName("Las excepciones del servicio llegan al cliente con su código y un cuerpo { message }")
    void actualizarServicio_servicioLanzaExcepcion_respondeCodigoDelHandler(RuntimeException excepcion,
            int estadoEsperado, String mensaje) throws Exception {
        // Arrange
        when(servicioService.update(any(Negocio.class), anyLong(), any(ServicioDTO.class))).thenThrow(excepcion);
        RequestPostProcessor admin = adminDelNegocio();

        // Act
        ResultActions resultado = mvc.perform(peticion("PUT", "/servicios/99", SERVICIO).with(admin));

        // Assert
        resultado.andExpect(status().is(estadoEsperado))
                .andExpect(jsonPath("$.message").value(mensaje));
    }

    // -------------------- Aislamiento multi-tenant y forma de la respuesta --------------------

    @Test
    @DisplayName("Aunque el cuerpo diga otro negocio, la categoría se crea en el negocio del token")
    void crearCategoria_cuerpoConOtroNegocio_usaNegocioDelToken() throws Exception {
        // Arrange
        CategoriaDTO creada = new CategoriaDTO();
        creada.setId(21L);
        creada.setNombre("Decoración");
        creada.setActiva(true);
        when(categoriaService.create(same(negocioDelToken), any(CategoriaDTO.class))).thenReturn(creada);
        String cuerpoConOtroNegocio = """
                {"nombre":"Decoración","negocioId":99,"negocio":{"id":99}}""";
        RequestPostProcessor admin = adminDelNegocio();

        // Act
        ResultActions resultado = mvc.perform(post("/categorias").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpoConOtroNegocio));

        // Assert
        resultado.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(21))
                .andExpect(jsonPath("$.nombre").value("Decoración"))
                .andExpect(jsonPath("$.descripcion").doesNotExist());
        verify(categoriaService).create(same(negocioDelToken),
                argThat(dto -> "Decoración".equals(dto.getNombre())));
    }

    @Test
    @DisplayName("GET /servicios devuelve lo que el servicio entrega para el negocio del token, sin campos nulos")
    void listarServicios_conPermiso_devuelveServiciosDelNegocioDelToken() throws Exception {
        // Arrange
        CategoriaDTO mobiliario = new CategoriaDTO();
        mobiliario.setId(3L);
        mobiliario.setNombre("Mobiliario");
        ServicioDTO mesa = new ServicioDTO();
        mesa.setId(12L);
        mesa.setNombre("Mesa imperial");
        mesa.setPrecio(new BigDecimal("1500.00"));
        mesa.setTipoCobro(TipoCobro.POR_EVENTO);
        mesa.setCategoria(mobiliario);
        when(servicioService.getAllByNegocio(7L)).thenReturn(List.of(mesa));
        RequestPostProcessor empleado = usuarioCon(List.of("ROLE_EMPLEADO", "PERM_SERVICIO_VER"));

        // Act
        ResultActions resultado = mvc.perform(get("/servicios").with(empleado));

        // Assert
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(12))
                .andExpect(jsonPath("$[0].precio").value(1500.00))
                .andExpect(jsonPath("$[0].tipoCobro").value("POR_EVENTO"))
                .andExpect(jsonPath("$[0].categoria.nombre").value("Mobiliario"))
                .andExpect(jsonPath("$[0].imagenUrl").doesNotExist());
    }

    @Test
    @DisplayName("GET /turnos envía el día como texto (SATURDAY) y las horas como HH:mm:ss, no como arreglos")
    void listarTurnos_autenticado_serializaDiaYHorasComoTexto() throws Exception {
        // Arrange
        TurnoDTO noche = new TurnoDTO();
        noche.setId(15L);
        noche.setDiaSemana(DayOfWeek.SATURDAY);
        noche.setTipoTurno("NOCHE");
        noche.setHoraInicio(LocalTime.of(18, 0));
        noche.setHoraFin(LocalTime.of(23, 30));
        when(turnoService.getAllByNegocio(7L)).thenReturn(List.of(noche));
        RequestPostProcessor admin = adminDelNegocio();

        // Act
        ResultActions resultado = mvc.perform(get("/turnos").with(admin));

        // Assert
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].diaSemana").value("SATURDAY"))
                .andExpect(jsonPath("$[0].horaInicio").value("18:00:00"))
                .andExpect(jsonPath("$[0].horaFin").value("23:30:00"));
    }
}