package mx.gestorsalon.controller;

import mx.gestorsalon.config.SecurityConfig;
import mx.gestorsalon.dto.DireccionDTO;
import mx.gestorsalon.dto.admin.CrearNegocioRequest;
import mx.gestorsalon.dto.admin.NegocioAdminResponse;
import mx.gestorsalon.dto.admin.UsuarioResponse;
import mx.gestorsalon.dto.auth.AuthResponse;
import mx.gestorsalon.dto.negocio.NegocioResponse;
import mx.gestorsalon.exception.RecursoDuplicadoException;
import mx.gestorsalon.model.enums.Permiso;
import mx.gestorsalon.security.CustomUserDetailsService;
import mx.gestorsalon.security.JwtService;
import mx.gestorsalon.service.AdminService;
import mx.gestorsalon.service.AuthService;
import mx.gestorsalon.service.ImageUploadService;
import mx.gestorsalon.service.NegocioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Capa web de la cuenta y la plataforma: login (/auth), alta de negocios por el
 * SUPERADMIN (/admin), perfil del propio negocio (/negocios/me) y subida de
 * imágenes (/images).
 *
 * Se usa la SecurityConfig REAL: /auth/login es la única ruta pública y /admin/**
 * exige ROLE_SUPERADMIN a nivel de URL. Los servicios son mocks.
 */
@WebMvcTest({AuthController.class, AdminController.class, NegocioController.class, ImageController.class})
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class CuentaControllerTest {

    private static final String CUERPO_INVALIDO = "La solicitud tiene un formato inválido o campos no reconocidos";

    private static final String LOGIN = """
            {"correo":"ana@laspalmas.mx","password":"Secreta123!"}""";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private AdminService adminService;
    @MockitoBean
    private NegocioService negocioService;
    @MockitoBean
    private ImageUploadService imageUploadService;
    // JwtAuthenticationFilter es un @Component que @WebMvcTest sí carga: sus dependencias van como mocks.
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    // -------------------- Ayudantes --------------------

    private static RequestPostProcessor superadmin() {
        return user("root@plataforma.mx").roles("SUPERADMIN");
    }

    /** El dueño de un salón: ROLE_ADMIN + todos los PERM_ (igual que CustomUserDetails). */
    private static RequestPostProcessor adminDelNegocio() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        for (Permiso permiso : Permiso.values()) {
            authorities.add(new SimpleGrantedAuthority("PERM_" + permiso.name()));
        }
        return user("ana@laspalmas.mx").authorities(authorities);
    }

    private static String solicitudNegocio(String nombre, String slug, String adminNombre, String adminCorreo,
            String adminPassword) {
        return """
                {"nombre":%s,"slug":%s,"adminNombre":%s,"adminCorreo":%s,"adminPassword":%s}"""
                .formatted(json(nombre), json(slug), json(adminNombre), json(adminCorreo), json(adminPassword));
    }

    private static String json(String valor) {
        return valor == null ? "null" : "\"" + valor + "\"";
    }

    private static NegocioResponse perfilLasPalmas() {
        DireccionDTO direccion = new DireccionDTO();
        direccion.setCalle("Juárez");
        direccion.setCiudad("Guasave");
        return NegocioResponse.builder()
                .id(7L)
                .nombre("Salón Las Palmas")
                .slug("salon-las-palmas")
                .activo(true)
                .telefono("6871234567")
                .direccion(direccion)
                .build();
    }

    // -------------------- Rutas privadas sin token --------------------

    @ParameterizedTest(name = "{0} {1} sin token -> 401")
    @CsvSource({
            "GET,  /auth/me",
            "GET,  /negocios/me",
            "PUT,  /negocios/me",
            "POST, /images/upload",
            "GET,  /admin/negocios",
            "POST, /admin/negocios"
    })
    @DisplayName("Fuera de /auth/login, toda ruta exige token: sin él responde 401 sin tocar los servicios")
    void rutaPrivada_sinToken_responde401(String metodo, String url) throws Exception {
        // Arrange
        MockHttpServletRequestBuilder anonima = request(HttpMethod.valueOf(metodo), url);

        // Act
        ResultActions resultado = mvc.perform(anonima);

        // Assert
        resultado.andExpect(status().isUnauthorized());
        verifyNoInteractions(authService, adminService, negocioService, imageUploadService);
    }

    // -------------------- /auth --------------------

    @Test
    @DisplayName("POST /auth/login es público y devuelve el token que genera el servicio")
    void login_sinToken_responde200ConToken() throws Exception {
        // Arrange
        when(authService.login(any())).thenReturn(new AuthResponse("jwt.de.prueba", "Ana López", "ADMIN"));

        // Act
        ResultActions resultado = mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(LOGIN));

        // Assert
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.de.prueba"))
                .andExpect(jsonPath("$.nombre").value("Ana López"))
                .andExpect(jsonPath("$.rol").value("ADMIN"));
        verify(authService).login(argThat(req ->
                "ana@laspalmas.mx".equals(req.getCorreo()) && "Secreta123!".equals(req.getPassword())));
    }

    @Test
    @DisplayName("POST /auth/login con credenciales incorrectas responde 401 con el mensaje del servicio")
    void login_credencialesIncorrectas_responde401() throws Exception {
        // Arrange
        when(authService.login(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos"));

        // Act
        ResultActions resultado = mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(LOGIN));

        // Assert
        resultado.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Correo o contraseña incorrectos"));
    }

    @Test
    @DisplayName("POST /auth/login con un JSON malformado responde 400 sin llamar al servicio")
    void login_jsonMalformado_responde400() throws Exception {
        // Arrange
        String cuerpoRoto = "{\"correo\":\"ana@laspalmas.mx\",";

        // Act
        ResultActions resultado = mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(cuerpoRoto));

        // Assert
        resultado.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(CUERPO_INVALIDO));
        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("GET /auth/me usa el correo de la sesión, no uno que mande el cliente")
    void me_autenticado_consultaConElCorreoDeLaSesion() throws Exception {
        // Arrange
        when(authService.getMe("ana@laspalmas.mx")).thenReturn(new AuthResponse("jwt.renovado", "Ana López", "ADMIN"));
        RequestPostProcessor ana = user("ana@laspalmas.mx").roles("ADMIN");

        // Act
        ResultActions resultado = mvc.perform(get("/auth/me").param("correo", "otro@intruso.mx").with(ana));

        // Assert
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.renovado"))
                .andExpect(jsonPath("$.nombre").value("Ana López"));
        verify(authService).getMe("ana@laspalmas.mx");
    }

    // -------------------- /admin (solo SUPERADMIN) --------------------

    @Test
    @DisplayName("POST /admin/negocios como SUPERADMIN responde 201 con el negocio y su admin inicial")
    void crearNegocio_superadmin_responde201() throws Exception {
        // Arrange
        NegocioAdminResponse creado = NegocioAdminResponse.builder()
                .negocio(perfilLasPalmas())
                .admin(UsuarioResponse.builder().id(10L).nombre("Ana López")
                        .correo("ana@laspalmas.mx").rol("ADMIN").build())
                .build();
        when(adminService.crearNegocioConAdmin(any(CrearNegocioRequest.class))).thenReturn(creado);
        String cuerpo = solicitudNegocio("Salón Las Palmas", "Salón Las Palmas", "Ana López",
                "ana@laspalmas.mx", "Secreta123!");
        RequestPostProcessor plataforma = superadmin();

        // Act
        ResultActions resultado = mvc.perform(post("/admin/negocios").with(plataforma)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));

        // Assert
        resultado.andExpect(status().isCreated())
                .andExpect(jsonPath("$.negocio.slug").value("salon-las-palmas"))
                .andExpect(jsonPath("$.admin.correo").value("ana@laspalmas.mx"))
                .andExpect(jsonPath("$.admin.rol").value("ADMIN"));
        verify(adminService).crearNegocioConAdmin(argThat(req ->
                "Salón Las Palmas".equals(req.getSlug()) && "Secreta123!".equals(req.getAdminPassword())));
    }

    @Test
    @DisplayName("GET /admin/negocios como SUPERADMIN lista todos los negocios de la plataforma")
    void listarNegocios_superadmin_responde200() throws Exception {
        // Arrange
        NegocioResponse otro = NegocioResponse.builder().id(8L).nombre("Quinta Real").slug("quinta-real").build();
        when(adminService.listarNegocios()).thenReturn(List.of(perfilLasPalmas(), otro));
        RequestPostProcessor plataforma = superadmin();

        // Act
        ResultActions resultado = mvc.perform(get("/admin/negocios").with(plataforma));

        // Assert
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].slug").value("quinta-real"));
    }

    @ParameterizedTest(name = "{0} /admin/negocios como {1} -> 403")
    @CsvSource({"GET, ADMIN", "POST, ADMIN", "GET, EMPLEADO", "POST, EMPLEADO"})
    @DisplayName("Un ADMIN o EMPLEADO de un salón no entra a /admin aunque tenga todos los PERM_")
    void admin_rolDeNegocio_responde403(String metodo, String rol) throws Exception {
        // Arrange
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + rol));
        for (Permiso permiso : Permiso.values()) {
            authorities.add(new SimpleGrantedAuthority("PERM_" + permiso.name()));
        }
        RequestPostProcessor usuarioDeSalon = user("dueno@laspalmas.mx").authorities(authorities);
        String cuerpo = solicitudNegocio("Salón Pirata", "salon-pirata", "Pirata", "pirata@correo.mx", "Secreta123!");

        // Act
        ResultActions resultado = mvc.perform(request(HttpMethod.valueOf(metodo), "/admin/negocios")
                .with(usuarioDeSalon)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));

        // Assert
        resultado.andExpect(status().isForbidden());
        verifyNoInteractions(adminService);
    }

    @ParameterizedTest(name = "{0} = [{1}] -> \"{2}\"")
    @CsvSource(nullValues = "NULL", value = {
            "nombre,        '   ',               El nombre del negocio es obligatorio",
            "slug,          '',                  El slug (enlace público) es obligatorio",
            "adminNombre,   NULL,                El nombre del administrador es obligatorio",
            "adminCorreo,   ana-sin-arroba,      El correo del administrador no tiene un formato válido",
            "adminPassword, corta,               La contraseña debe tener al menos 8 caracteres"
    })
    @DisplayName("POST /admin/negocios con un campo inválido responde 400 con su mensaje y no crea nada")
    void crearNegocio_campoInvalido_responde400(String campo, String valor, String mensaje) throws Exception {
        // Arrange
        String cuerpo = solicitudNegocio(
                campo.equals("nombre") ? valor : "Salón Las Palmas",
                campo.equals("slug") ? valor : "salon-las-palmas",
                campo.equals("adminNombre") ? valor : "Ana López",
                campo.equals("adminCorreo") ? valor : "ana@laspalmas.mx",
                campo.equals("adminPassword") ? valor : "Secreta123!");
        RequestPostProcessor plataforma = superadmin();

        // Act
        ResultActions resultado = mvc.perform(post("/admin/negocios").with(plataforma)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));

        // Assert
        resultado.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(mensaje));
        verifyNoInteractions(adminService);
    }

    @Test
    @DisplayName("POST /admin/negocios con un slug ya usado responde 409 con el mensaje del servicio")
    void crearNegocio_slugDuplicado_responde409() throws Exception {
        // Arrange
        when(adminService.crearNegocioConAdmin(any(CrearNegocioRequest.class)))
                .thenThrow(new RecursoDuplicadoException("Ya existe un negocio con ese slug"));
        String cuerpo = solicitudNegocio("Salón Las Palmas", "salon-las-palmas", "Ana López",
                "ana@laspalmas.mx", "Secreta123!");
        RequestPostProcessor plataforma = superadmin();

        // Act
        ResultActions resultado = mvc.perform(post("/admin/negocios").with(plataforma)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));

        // Assert
        resultado.andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe un negocio con ese slug"));
    }

    // -------------------- /negocios/me --------------------

    @Test
    @DisplayName("GET /negocios/me devuelve el perfil del negocio de la sesión, sin campos nulos")
    void getMiNegocio_autenticado_devuelvePerfil() throws Exception {
        // Arrange
        when(negocioService.getMiNegocio()).thenReturn(perfilLasPalmas());
        RequestPostProcessor admin = adminDelNegocio();

        // Act
        ResultActions resultado = mvc.perform(get("/negocios/me").with(admin));

        // Assert
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.slug").value("salon-las-palmas"))
                .andExpect(jsonPath("$.direccion.ciudad").value("Guasave"))
                .andExpect(jsonPath("$.logoUrl").doesNotExist())
                .andExpect(jsonPath("$.direccion.codigoPostal").doesNotExist());
    }

    @Test
    @DisplayName("PUT /negocios/me entrega al servicio los datos y la dirección anidada")
    void updateMiNegocio_datosValidos_responde200() throws Exception {
        // Arrange
        when(negocioService.updateMiNegocio(any())).thenReturn(perfilLasPalmas());
        RequestPostProcessor admin = adminDelNegocio();
        String cuerpo = """
                {"nombre":"Salón Las Palmas","telefono":"6871234567","email":"contacto@laspalmas.mx",
                 "direccion":{"calle":"Juárez","ciudad":"Guasave"}}""";

        // Act
        ResultActions resultado = mvc.perform(put("/negocios/me").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));

        // Assert
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Salón Las Palmas"));
        verify(negocioService).updateMiNegocio(argThat(req ->
                "contacto@laspalmas.mx".equals(req.getEmail())
                        && "Guasave".equals(req.getDireccion().getCiudad())));
    }

    @Test
    @DisplayName("PUT /negocios/me con un correo inválido responde 400 y no actualiza")
    void updateMiNegocio_correoInvalido_responde400() throws Exception {
        // Arrange
        RequestPostProcessor admin = adminDelNegocio();

        // Act
        ResultActions resultado = mvc.perform(put("/negocios/me").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"contacto-sin-arroba\"}"));

        // Assert
        resultado.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El correo electrónico no tiene un formato válido"));
        verifyNoInteractions(negocioService);
    }

    @Test
    @DisplayName("PUT /negocios/me con un slug ocupado responde 400 con la regla de negocio")
    void updateMiNegocio_slugOcupado_responde400() throws Exception {
        // Arrange
        when(negocioService.updateMiNegocio(any()))
                .thenThrow(new IllegalArgumentException("Esta dirección ya está en uso por otro negocio"));
        RequestPostProcessor admin = adminDelNegocio();

        // Act
        ResultActions resultado = mvc.perform(put("/negocios/me").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"slug\":\"quinta-real\"}"));

        // Assert
        resultado.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Esta dirección ya está en uso por otro negocio"));
    }

    // -------------------- /images/upload --------------------

    @Test
    @DisplayName("POST /images/upload devuelve { url } con la URL segura que entrega el servicio")
    void uploadImage_archivoValido_devuelveUrl() throws Exception {
        // Arrange
        MockMultipartFile logo = new MockMultipartFile("file", "logo.png", "image/png", new byte[] {1, 2, 3});
        when(imageUploadService.uploadImage(any(MultipartFile.class)))
                .thenReturn("https://res.cloudinary.com/demo/image/upload/servicios/abc.png");
        RequestPostProcessor admin = adminDelNegocio();

        // Act
        ResultActions resultado = mvc.perform(multipart("/images/upload").file(logo).with(admin));

        // Assert
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://res.cloudinary.com/demo/image/upload/servicios/abc.png"));
        verify(imageUploadService).uploadImage(argThat(archivo -> "logo.png".equals(archivo.getOriginalFilename())));
    }

    @ParameterizedTest(name = "{0} \"{1}\"")
    @CsvSource(delimiter = '|', value = {
            "400 | El archivo está vacío",
            "400 | Solo se permiten imágenes JPEG, PNG o WEBP.",
            "500 | Error al procesar la imagen"
    })
    @DisplayName("Si el servicio rechaza la imagen, se conserva su código y su mensaje")
    void uploadImage_servicioRechaza_conservaCodigoYMensaje(int codigo, String mensaje) throws Exception {
        // Arrange
        MockMultipartFile archivo = new MockMultipartFile("file", "factura.pdf", "application/pdf", new byte[] {9});
        when(imageUploadService.uploadImage(any(MultipartFile.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.valueOf(codigo), mensaje));
        RequestPostProcessor admin = adminDelNegocio();

        // Act
        ResultActions resultado = mvc.perform(multipart("/images/upload").file(archivo).with(admin));

        // Assert
        resultado.andExpect(status().is(codigo))
                .andExpect(jsonPath("$.message").value(mensaje));
    }
}