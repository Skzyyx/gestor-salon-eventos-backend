package mx.gestorsalon.security;

import mx.gestorsalon.model.RolNegocio;
import mx.gestorsalon.model.Usuario;
import mx.gestorsalon.model.enums.Permiso;
import mx.gestorsalon.model.enums.Rol;
import mx.gestorsalon.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    private static final String LUIS = "luis.perez@laspalmas.mx";

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("loadUserByUsername carga al usuario con sus permisos en una sola consulta y lo envuelve para Spring Security")
    void loadUserByUsername_correoRegistrado_devuelveUsuarioEnvuelto() {
        // Arrange
        RolNegocio recepcionista = new RolNegocio();
        recepcionista.setPermisos(EnumSet.of(Permiso.CLIENTE_VER));
        Usuario luis = new Usuario();
        luis.setId(20L);
        luis.setCorreo(LUIS);
        luis.setPasswordHash("hash-bcrypt");
        luis.setRol(Rol.EMPLEADO);
        luis.setRolNegocio(recepcionista);
        when(usuarioRepository.findByCorreoConPermisos(LUIS)).thenReturn(Optional.of(luis));

        // Act
        UserDetails detalles = customUserDetailsService.loadUserByUsername(LUIS);

        // Assert
        CustomUserDetails envuelto = assertInstanceOf(CustomUserDetails.class, detalles);
        assertSame(luis, envuelto.getUsuario());
        assertEquals(LUIS, envuelto.getUsername());
        assertEquals("hash-bcrypt", envuelto.getPassword());
        assertTrue(envuelto.getAuthorities().stream().anyMatch(a -> "PERM_CLIENTE_VER".equals(a.getAuthority())));
        verify(usuarioRepository).findByCorreoConPermisos(LUIS);
        verifyNoMoreInteractions(usuarioRepository);
    }

    @ParameterizedTest(name = "correo = {0}")
    @DisplayName("loadUserByUsername lanza UsernameNotFoundException con el correo buscado cuando no existe")
    @ValueSource(strings = {"nadie@laspalmas.mx", "ex.empleado@quintareal.mx", ""})
    void loadUserByUsername_correoDesconocido_lanzaUsernameNotFound(String correo) {
        // Arrange
        when(usuarioRepository.findByCorreoConPermisos(correo)).thenReturn(Optional.empty());

        // Act
        UsernameNotFoundException error = assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername(correo));

        // Assert
        assertEquals("Usuario no encontrado: " + correo, error.getMessage());
        verify(usuarioRepository).findByCorreoConPermisos(correo);
        verifyNoMoreInteractions(usuarioRepository);
    }
}