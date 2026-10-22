package mx.gestorsalon.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.ServletException;
import mx.gestorsalon.model.RolNegocio;
import mx.gestorsalon.model.Usuario;
import mx.gestorsalon.model.enums.Permiso;
import mx.gestorsalon.model.enums.Rol;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJsdWlzIn0.firma";
    private static final String LUIS = "luis.perez@laspalmas.mx";
    private static final String IP_CLIENTE = "201.175.10.20";

    @Mock
    private JwtService jwtService;
    @Mock
    private CustomUserDetailsService userDetailsService;

    @InjectMocks
    private JwtAuthenticationFilter filtro;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        request = new MockHttpServletRequest("GET", "/api/turnos");
        request.setRemoteAddr(IP_CLIENTE);
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    /** Luis (id 20) es EMPLEADO con rol Recepcionista: solo puede ver clientes y crear turnos. */
    private static CustomUserDetails recepcionistaLuis() {
        RolNegocio recepcionista = new RolNegocio();
        recepcionista.setNombre("Recepcionista");
        recepcionista.setPermisos(EnumSet.of(Permiso.CLIENTE_VER, Permiso.TURNO_CREAR));
        Usuario luis = new Usuario();
        luis.setId(20L);
        luis.setCorreo(LUIS);
        luis.setPasswordHash("hash-bcrypt");
        luis.setRol(Rol.EMPLEADO);
        luis.setRolNegocio(recepcionista);
        return new CustomUserDetails(luis);
    }

    private static Set<String> nombres(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }

    private void conAutorizacion(String valor) {
        request.addHeader("Authorization", valor);
    }

    /** La petición siguió su camino exactamente una vez (MockFilterChain no admite dos llamadas). */
    private void assertLaPeticionContinuo() {
        assertSame(request, chain.getRequest());
        assertSame(response, chain.getResponse());
    }

    static Stream<Arguments> fallosAlLeerElToken() {
        return Stream.of(
                Arguments.of(Named.of("token expirado",
                        new ExpiredJwtException(null, null, "JWT expired 3600000 milliseconds ago"))),
                Arguments.of(Named.of("token mal formado",
                        new MalformedJwtException("Invalid compact JWT string"))),
                Arguments.of(Named.of("firma alterada o de otra clave",
                        new SignatureException("JWT signature does not match locally computed signature"))),
                Arguments.of(Named.of("token sin firmar",
                        new UnsupportedJwtException("Unprotected JWTs are not supported"))),
                Arguments.of(Named.of("token vacío",
                        new IllegalArgumentException("CharSequence cannot be null or empty."))));
    }

    @ParameterizedTest(name = "Authorization = [{0}]")
    @DisplayName("sin encabezado o con un esquema distinto de 'Bearer ' la petición sigue sin autenticar y no se lee ningún token")
    @NullSource
    @ValueSource(strings = {"Basic bHVpczpzZWNyZXRh", "bearer " + TOKEN, "Token " + TOKEN, "Bearer", "Bearer" + TOKEN, ""})
    void doFilter_sinEncabezadoBearer_continuaSinAutenticar(String encabezado) throws ServletException, IOException {
        // Arrange
        if (encabezado != null) {
            conAutorizacion(encabezado);
        }

        // Act
        filtro.doFilter(request, response, chain);

        // Assert
        assertLaPeticionContinuo();
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    @DisplayName("con un token válido autentica al usuario cargado de la BD, con sus authorities y los datos de la petición")
    void doFilter_tokenValido_autenticaAlUsuarioDelToken() throws ServletException, IOException {
        // Arrange
        CustomUserDetails luis = recepcionistaLuis();
        conAutorizacion("Bearer " + TOKEN);
        when(jwtService.extractUsername(TOKEN)).thenReturn(LUIS);
        when(userDetailsService.loadUserByUsername(LUIS)).thenReturn(luis);
        when(jwtService.isTokenValid(TOKEN, LUIS)).thenReturn(true);

        // Act
        filtro.doFilter(request, response, chain);

        // Assert
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        UsernamePasswordAuthenticationToken pase = assertInstanceOf(UsernamePasswordAuthenticationToken.class, autenticacion);
        assertTrue(pase.isAuthenticated());
        assertSame(luis, pase.getPrincipal());
        assertNull(pase.getCredentials());
        assertEquals(Set.of("ROLE_EMPLEADO", "PERM_CLIENTE_VER", "PERM_TURNO_CREAR"), nombres(pase.getAuthorities()));
        WebAuthenticationDetails detalles = assertInstanceOf(WebAuthenticationDetails.class, pase.getDetails());
        assertEquals(IP_CLIENTE, detalles.getRemoteAddress());
        assertLaPeticionContinuo();
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("quita 'Bearer ' y los espacios sobrantes antes de leer el token")
    void doFilter_tokenConEspacios_seLeeRecortado() throws ServletException, IOException {
        // Arrange
        CustomUserDetails luis = recepcionistaLuis();
        conAutorizacion("Bearer    " + TOKEN + "  ");
        when(jwtService.extractUsername(TOKEN)).thenReturn(LUIS);
        when(userDetailsService.loadUserByUsername(LUIS)).thenReturn(luis);
        when(jwtService.isTokenValid(TOKEN, LUIS)).thenReturn(true);

        // Act
        filtro.doFilter(request, response, chain);

        // Assert
        assertSame(luis, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(jwtService).extractUsername(TOKEN);
        verify(jwtService).isTokenValid(TOKEN, LUIS);
        assertLaPeticionContinuo();
    }

    @Test
    @DisplayName("si el token no es válido para el usuario cargado (otro correo o vencido) no se autentica")
    void doFilter_tokenNoValidoParaElUsuario_noAutentica() throws ServletException, IOException {
        // Arrange
        conAutorizacion("Bearer " + TOKEN);
        when(jwtService.extractUsername(TOKEN)).thenReturn(LUIS);
        when(userDetailsService.loadUserByUsername(LUIS)).thenReturn(recepcionistaLuis());
        when(jwtService.isTokenValid(TOKEN, LUIS)).thenReturn(false);

        // Act
        filtro.doFilter(request, response, chain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertLaPeticionContinuo();
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("si la librería JWT rechaza el token, la petición sigue sin autenticar y sin error 500")
    @MethodSource("fallosAlLeerElToken")
    void doFilter_tokenRechazadoPorJwt_continuaSinAutenticar(RuntimeException fallo) throws ServletException, IOException {
        // Arrange
        conAutorizacion("Bearer " + TOKEN);
        when(jwtService.extractUsername(TOKEN)).thenThrow(fallo);

        // Act
        filtro.doFilter(request, response, chain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertLaPeticionContinuo();
        assertEquals(200, response.getStatus());
        verify(jwtService, never()).isTokenValid(anyString(), anyString());
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("un token firmado de un usuario que ya no existe en la BD no autentica a nadie")
    void doFilter_usuarioDelTokenYaNoExiste_continuaSinAutenticar() throws ServletException, IOException {
        // Arrange
        conAutorizacion("Bearer " + TOKEN);
        when(jwtService.extractUsername(TOKEN)).thenReturn(LUIS);
        when(userDetailsService.loadUserByUsername(LUIS))
                .thenThrow(new UsernameNotFoundException("Usuario no encontrado: " + LUIS));

        // Act
        filtro.doFilter(request, response, chain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertLaPeticionContinuo();
        verify(jwtService, never()).isTokenValid(anyString(), anyString());
    }

    @Test
    @DisplayName("si el token no trae correo (subject vacío) no se consulta la BD ni se autentica")
    void doFilter_tokenSinCorreo_noCargaUsuario() throws ServletException, IOException {
        // Arrange
        conAutorizacion("Bearer " + TOKEN);
        when(jwtService.extractUsername(TOKEN)).thenReturn(null);

        // Act
        filtro.doFilter(request, response, chain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertLaPeticionContinuo();
        verifyNoInteractions(userDetailsService);
        verify(jwtService, never()).isTokenValid(anyString(), anyString());
    }

    @Test
    @DisplayName("si el hilo ya trae una autenticación no se recarga el usuario ni se reemplaza el pase existente")
    void doFilter_contextoYaAutenticado_noRecargaUsuario() throws ServletException, IOException {
        // Arrange
        UsernamePasswordAuthenticationToken existente = UsernamePasswordAuthenticationToken.authenticated(
                "ana.lopez@laspalmas.mx", null, AuthorityUtils.createAuthorityList("ROLE_ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(existente);
        conAutorizacion("Bearer " + TOKEN);
        when(jwtService.extractUsername(TOKEN)).thenReturn(LUIS);

        // Act
        filtro.doFilter(request, response, chain);

        // Assert
        assertSame(existente, SecurityContextHolder.getContext().getAuthentication());
        assertLaPeticionContinuo();
        verifyNoInteractions(userDetailsService);
        verify(jwtService, never()).isTokenValid(anyString(), anyString());
    }
}