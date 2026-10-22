package mx.gestorsalon.security;

import mx.gestorsalon.model.RolNegocio;
import mx.gestorsalon.model.Usuario;
import mx.gestorsalon.model.enums.Permiso;
import mx.gestorsalon.model.enums.Rol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.GrantedAuthority;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomUserDetailsTest {

    private static Usuario usuario(Rol rol, RolNegocio rolNegocio) {
        Usuario usuario = new Usuario();
        usuario.setId(20L);
        usuario.setNombre("Luis Pérez");
        usuario.setCorreo("luis.perez@laspalmas.mx");
        usuario.setPasswordHash("$2a$10$hashDeLuis");
        usuario.setRol(rol);
        usuario.setRolNegocio(rolNegocio);
        return usuario;
    }

    private static RolNegocio rolNegocio(Set<Permiso> permisos) {
        RolNegocio rol = new RolNegocio();
        rol.setNombre("Recepcionista");
        rol.setPermisos(permisos);
        return rol;
    }

    private static List<String> authorities(CustomUserDetails detalles) {
        return detalles.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }

    private static Set<String> comoPerm(Set<Permiso> permisos) {
        return permisos.stream().map(p -> "PERM_" + p.name()).collect(Collectors.toSet());
    }

    static Stream<Arguments> permisosDeRolNegocio() {
        return Stream.of(
                Arguments.of(Named.of("rol sin permisos", EnumSet.noneOf(Permiso.class))),
                Arguments.of(Named.of("solo ver clientes", EnumSet.of(Permiso.CLIENTE_VER))),
                Arguments.of(Named.of("recepcionista", EnumSet.of(Permiso.SERVICIO_VER, Permiso.CATEGORIA_VER,
                        Permiso.PAQUETE_VER, Permiso.CLIENTE_VER, Permiso.CLIENTE_CREAR, Permiso.TURNO_VER,
                        Permiso.TURNO_CREAR, Permiso.NEGOCIO_VER_PERFIL))),
                Arguments.of(Named.of("catálogo completo", EnumSet.allOf(Permiso.class))));
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("cada cuenta lleva exactamente una authority de rol, y es la de su propio rol")
    @EnumSource(Rol.class)
    void getAuthorities_cualquierRol_incluyeSoloSuAuthorityDeRol(Rol rol) {
        // Arrange
        CustomUserDetails detalles = new CustomUserDetails(usuario(rol, null));

        // Act
        List<String> nombres = authorities(detalles);

        // Assert
        List<String> deRol = nombres.stream().filter(a -> a.startsWith("ROLE_")).toList();
        assertEquals(List.of("ROLE_" + rol.name()), deRol);
    }

    @Test
    @DisplayName("el SUPERADMIN solo tiene ROLE_SUPERADMIN: ningún permiso de negocio, aunque tuviera un rol asignado")
    void getAuthorities_superadmin_soloRoleSuperadmin() {
        // Arrange
        RolNegocio gerente = rolNegocio(EnumSet.of(Permiso.CLIENTE_VER, Permiso.EQUIPO_GESTIONAR));
        CustomUserDetails detalles = new CustomUserDetails(usuario(Rol.SUPERADMIN, gerente));

        // Act
        List<String> nombres = authorities(detalles);

        // Assert
        assertEquals(List.of("ROLE_SUPERADMIN"), nombres);
    }

    @Test
    @DisplayName("el ADMIN recibe ROLE_ADMIN y un PERM_ por cada permiso del catálogo, sin depender de un rol de negocio")
    void getAuthorities_admin_recibeTodoElCatalogo() {
        // Arrange
        CustomUserDetails detalles = new CustomUserDetails(usuario(Rol.ADMIN, null));
        Set<String> esperadas = comoPerm(EnumSet.allOf(Permiso.class));
        esperadas.add("ROLE_ADMIN");

        // Act
        List<String> nombres = authorities(detalles);

        // Assert
        assertEquals(Permiso.values().length + 1, nombres.size());
        assertEquals(esperadas, new HashSet<>(nombres));
        assertTrue(Arrays.stream(Permiso.values()).allMatch(p -> nombres.contains("PERM_" + p.name())));
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("el EMPLEADO recibe ROLE_EMPLEADO y únicamente los permisos de su rol de negocio")
    @MethodSource("permisosDeRolNegocio")
    void getAuthorities_empleado_recibeSoloLosPermisosDeSuRol(Set<Permiso> permisos) {
        // Arrange
        CustomUserDetails detalles = new CustomUserDetails(usuario(Rol.EMPLEADO, rolNegocio(permisos)));
        Set<String> esperadas = comoPerm(permisos);
        esperadas.add("ROLE_EMPLEADO");

        // Act
        List<String> nombres = authorities(detalles);

        // Assert
        assertEquals(permisos.size() + 1, nombres.size());
        assertEquals(esperadas, new HashSet<>(nombres));
    }

    @Test
    @DisplayName("un EMPLEADO que quedó sin rol de negocio solo conserva ROLE_EMPLEADO")
    void getAuthorities_empleadoSinRolNegocio_soloRoleEmpleado() {
        // Arrange
        CustomUserDetails detalles = new CustomUserDetails(usuario(Rol.EMPLEADO, null));

        // Act
        List<String> nombres = authorities(detalles);

        // Assert
        assertEquals(List.of("ROLE_EMPLEADO"), nombres);
    }

    @ParameterizedTest(name = "activo = {0}")
    @DisplayName("isEnabled refleja si la cuenta está activa, así una cuenta dada de baja no puede iniciar sesión")
    @ValueSource(booleans = {true, false})
    void isEnabled_sigueElEstadoActivoDelUsuario(boolean activo) {
        // Arrange
        Usuario luis = usuario(Rol.EMPLEADO, null);
        luis.setActivo(activo);
        CustomUserDetails detalles = new CustomUserDetails(luis);

        // Act
        boolean habilitada = detalles.isEnabled();

        // Assert
        assertEquals(activo, habilitada);
    }

    @Test
    @DisplayName("expone el correo como username, el hash como password y no marca la cuenta como vencida ni bloqueada")
    void datosDeCuenta_usuarioActivo_seTomanDelUsuario() {
        // Arrange
        Usuario luis = usuario(Rol.EMPLEADO, null);

        // Act
        CustomUserDetails detalles = new CustomUserDetails(luis);

        // Assert
        assertAll(
                () -> assertSame(luis, detalles.getUsuario()),
                () -> assertEquals("luis.perez@laspalmas.mx", detalles.getUsername()),
                () -> assertEquals("$2a$10$hashDeLuis", detalles.getPassword()),
                () -> assertTrue(detalles.isAccountNonExpired()),
                () -> assertTrue(detalles.isAccountNonLocked()),
                () -> assertTrue(detalles.isCredentialsNonExpired()));
    }
}