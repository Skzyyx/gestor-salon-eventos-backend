package mx.gestorsalon.service;

import mx.gestorsalon.dto.auth.AuthRequest;
import mx.gestorsalon.dto.auth.AuthResponse;
import mx.gestorsalon.model.Negocio;
import mx.gestorsalon.model.Usuario;
import mx.gestorsalon.model.enums.Rol;
import mx.gestorsalon.repository.UsuarioRepository;
import mx.gestorsalon.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String PASSWORD = "Secreta123!";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private static AuthRequest credenciales(String correo, String password) {
        AuthRequest request = new AuthRequest();
        request.setCorreo(correo);
        request.setPassword(password);
        return request;
    }

    private static Usuario usuario(String correo, String nombre, Rol rol, Long negocioId) {
        Usuario usuario = new Usuario();
        usuario.setId(5L);
        usuario.setCorreo(correo);
        usuario.setNombre(nombre);
        usuario.setRol(rol);
        if (negocioId != null) {
            Negocio negocio = new Negocio();
            negocio.setId(negocioId);
            usuario.setNegocio(negocio);
        }
        return usuario;
    }

    static Stream<Arguments> fallosDeAutenticacion() {
        return Stream.of(
                Arguments.of(Named.of("contraseña incorrecta", new BadCredentialsException("Bad credentials"))),
                Arguments.of(Named.of("correo inexistente", new UsernameNotFoundException("Usuario no encontrado"))),
                Arguments.of(Named.of("cuenta dada de baja", new DisabledException("User is disabled"))),
                Arguments.of(Named.of("cuenta bloqueada", new LockedException("User account is locked"))),
                Arguments.of(Named.of("cuenta vencida", new AccountExpiredException("User account has expired"))),
                Arguments.of(Named.of("credenciales vencidas",
                        new CredentialsExpiredException("User credentials have expired"))));
    }

    @ParameterizedTest(name = "{1} ({2}) con negocio {3}")
    @DisplayName("login valida credenciales y emite un token con el negocio del usuario (null para el SUPERADMIN)")
    @CsvSource({
            "ana.lopez@laspalmas.mx,    Ana López,          ADMIN,      7",
            "luis.perez@laspalmas.mx,   Luis Pérez,         EMPLEADO,   7",
            "soporte@gestorsalon.com,   Soporte Plataforma, SUPERADMIN,  "
    })
    void login_credencialesValidas_devuelveTokenConSuNegocio(String correo, String nombre, Rol rol, Long negocioId) {
        // Arrange
        AuthRequest request = credenciales(correo, PASSWORD);
        when(usuarioRepository.findByCorreo(correo)).thenReturn(Optional.of(usuario(correo, nombre, rol, negocioId)));
        when(jwtService.generateToken(correo, negocioId)).thenReturn("jwt-de-" + rol.name());

        // Act
        AuthResponse respuesta = authService.login(request);

        // Assert
        ArgumentCaptor<Authentication> intento = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(intento.capture());
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) intento.getValue();
        assertEquals(correo, token.getPrincipal());
        assertEquals(PASSWORD, token.getCredentials());
        assertEquals("jwt-de-" + rol.name(), respuesta.getToken());
        assertEquals(nombre, respuesta.getNombre());
        assertEquals(rol.name(), respuesta.getRol());
        verify(jwtService).generateToken(correo, negocioId);
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("login responde siempre 401 con el mismo mensaje genérico, sin revelar la causa ni emitir token")
    @MethodSource("fallosDeAutenticacion")
    void login_credencialesInvalidas_lanza401(AuthenticationException fallo) {
        // Arrange
        AuthRequest request = credenciales("ana.lopez@laspalmas.mx", "incorrecta");
        when(authenticationManager.authenticate(any(Authentication.class))).thenThrow(fallo);

        // Act
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> authService.login(request));

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        assertEquals("Correo o contraseña incorrectos", error.getReason());
        verifyNoInteractions(usuarioRepository, jwtService);
    }

    @ParameterizedTest(name = "{1} ({2}) con negocio {3}")
    @DisplayName("getMe devuelve los datos del usuario autenticado con un token renovado sin pedir contraseña")
    @CsvSource({
            "ana.lopez@laspalmas.mx,  Ana López,          ADMIN,      7",
            "soporte@gestorsalon.com, Soporte Plataforma, SUPERADMIN,  "
    })
    void getMe_usuarioExistente_devuelveSusDatosConTokenNuevo(String correo, String nombre, Rol rol, Long negocioId) {
        // Arrange
        when(usuarioRepository.findByCorreo(correo)).thenReturn(Optional.of(usuario(correo, nombre, rol, negocioId)));
        when(jwtService.generateToken(correo, negocioId)).thenReturn("jwt-renovado");

        // Act
        AuthResponse respuesta = authService.getMe(correo);

        // Assert
        assertEquals("jwt-renovado", respuesta.getToken());
        assertEquals(nombre, respuesta.getNombre());
        assertEquals(rol.name(), respuesta.getRol());
        verify(jwtService).generateToken(correo, negocioId);
        verifyNoInteractions(authenticationManager);
    }

    @Test
    @DisplayName("getMe responde 404 'Usuario no encontrado' si el correo del token ya no existe")
    void getMe_correoDesconocido_lanza404() {
        // Arrange
        when(usuarioRepository.findByCorreo("borrado@laspalmas.mx")).thenReturn(Optional.empty());

        // Act
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> authService.getMe("borrado@laspalmas.mx"));

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        assertEquals("Usuario no encontrado", error.getReason());
        verifyNoInteractions(jwtService, authenticationManager);
    }
}