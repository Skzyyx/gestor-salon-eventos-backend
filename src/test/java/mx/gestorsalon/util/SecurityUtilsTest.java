package mx.gestorsalon.util;

import mx.gestorsalon.model.Negocio;
import mx.gestorsalon.model.Usuario;
import mx.gestorsalon.model.enums.Rol;
import mx.gestorsalon.repository.UsuarioRepository;
import mx.gestorsalon.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityUtilsTest {

    private static final Long ID_ANA = 5L;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private SecurityUtils securityUtils;

    @BeforeEach
    void limpiarAntes() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void limpiarDespues() {
        SecurityContextHolder.clearContext();
    }

    private static Negocio negocioConId(Long id) {
        Negocio n = new Negocio();
        n.setId(id);
        n.setNombre("Salón Las Palmas");
        return n;
    }

    private static Usuario usuario(Rol rol, Negocio negocio, String nombre) {
        Usuario usuario = new Usuario();
        usuario.setId(ID_ANA);
        usuario.setNombre(nombre);
        usuario.setCorreo("ana.lopez@laspalmas.mx");
        usuario.setRol(rol);
        usuario.setNegocio(negocio);
        return usuario;
    }

    /** Simula lo que deja el JwtAuthenticationFilter: el principal es la copia cargada al inicio del request. */
    private static void autenticarComo(Usuario principal) {
        CustomUserDetails detalles = new CustomUserDetails(principal);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(detalles, null, detalles.getAuthorities()));
    }

    @Test
    @DisplayName("getUsuarioActual recarga al usuario desde la BD y devuelve la copia fresca, no la del token")
    void getUsuarioActual_usuarioAutenticado_devuelveLaCopiaFrescaDeLaBd() {
        // Arrange
        Usuario copiaDelRequest = usuario(Rol.ADMIN, negocioConId(1L), "Ana López");
        Usuario copiaFresca = usuario(Rol.ADMIN, negocioConId(1L), "Ana López Ruiz");
        copiaFresca.setActivo(false);
        autenticarComo(copiaDelRequest);
        when(usuarioRepository.findById(ID_ANA)).thenReturn(Optional.of(copiaFresca));

        // Act
        Usuario actual = securityUtils.getUsuarioActual();

        // Assert
        assertSame(copiaFresca, actual);
        assertNotSame(copiaDelRequest, actual);
        assertEquals("Ana López Ruiz", actual.getNombre());
        assertFalse(actual.getActivo());
        verify(usuarioRepository).findById(ID_ANA);
    }

    @Test
    @DisplayName("getUsuarioActual falla con 'Usuario no encontrado' si la cuenta se borró después de emitir el token")
    void getUsuarioActual_usuarioBorrado_lanzaExcepcion() {
        // Arrange
        autenticarComo(usuario(Rol.ADMIN, negocioConId(1L), "Ana López"));
        when(usuarioRepository.findById(ID_ANA)).thenReturn(Optional.empty());

        // Act
        RuntimeException error = assertThrows(RuntimeException.class, () -> securityUtils.getUsuarioActual());

        // Assert
        assertEquals("Usuario no encontrado", error.getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("getNegocioActual devuelve el negocio de la copia fresca del usuario cuando pertenece a uno")
    @EnumSource(value = Rol.class, names = {"ADMIN", "EMPLEADO"})
    void getNegocioActual_usuarioConNegocio_devuelveSuNegocio(Rol rol) {
        // Arrange
        Negocio negocioActual = negocioConId(1L);
        autenticarComo(usuario(rol, negocioConId(1L), "Ana López"));
        when(usuarioRepository.findById(ID_ANA)).thenReturn(Optional.of(usuario(rol, negocioActual, "Ana López")));

        // Act
        Negocio negocio = securityUtils.getNegocioActual();

        // Assert
        assertSame(negocioActual, negocio);
    }

    @Test
    @DisplayName("getNegocioActual rechaza al SUPERADMIN, que no pertenece a ningún negocio")
    void getNegocioActual_superadminSinNegocio_lanzaIllegalState() {
        // Arrange
        Usuario superadmin = usuario(Rol.SUPERADMIN, null, "Soporte Plataforma");
        autenticarComo(superadmin);
        when(usuarioRepository.findById(ID_ANA)).thenReturn(Optional.of(superadmin));

        // Act
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> securityUtils.getNegocioActual());

        // Assert
        assertEquals("El usuario actual no pertenece a ningún negocio (¿es SUPERADMIN?).", error.getMessage());
    }
}